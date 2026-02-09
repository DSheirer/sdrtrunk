/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.audio.playback;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.audio.AudioEvent;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import java.util.Arrays;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the incoming audio segments and provides access to audio buffers from those segments.
 */
public class AudioChannel implements Listener<IdentifierUpdateNotification>
{
    /**
     * Quantity of audio samples to deliver per getAudio() method invocation, equivalent to 20 milliseconds at an 8 kHz
     * sample rate.
     */
    public static final int SAMPLES_PER_INTERVAL = 160;
    private static final Logger LOGGER = LoggerFactory.getLogger(AudioChannel.class);
    private final AudioBuffer mAudioBuffer = new AudioBuffer();
    private final Broadcaster<AudioEvent> mAudioEventBroadcaster = new Broadcaster<>();
    private final LinkedTransferQueue<AudioSegment> mAudioSegmentQueue = new LinkedTransferQueue<>();
    private final UserPreferences mUserPreferences;
    private final String mChannelName;

    private AudioSegment mCurrentAudioSegment;
    private Listener<IdentifierCollection> mIdentifierCollectionListener;
    private boolean mDropDuplicates;
    private boolean mMetadataSent = false;
    private boolean mMuted;
    private boolean mDisabled;
    private float[] mAudioSegmentStartTone;
    private float[] mAudioSegmentDropTone;
    private int mCurrentBufferIndex = -1;
    private int mNoAudioFromSegmentIntervalCount = 0;

    /**
     * Constructs an instance
     */
    public AudioChannel(UserPreferences userPreferences, String channelName)
    {
        mUserPreferences = userPreferences;
        mChannelName = channelName;

        mDropDuplicates = mUserPreferences.getCallManagementPreference().isDuplicatePlaybackSuppressionEnabled();
        updateToneInsertionAudioClips();

        //Register to receive preference update notifications so we can update the preference items
        MyEventBus.getGlobalEventBus().register(this);
    }

    /**
     * Enables or disables this audio channel.
     * @param disabled state
     */
    public void setDisabled(boolean disabled)
    {
        mDisabled = disabled;

        if(disabled)
        {
            AudioSegment segment = mAudioSegmentQueue.poll();

            while(segment != null)
            {
                segment.decrementConsumerCount();
                segment = mAudioSegmentQueue.poll();
            }
        }
    }

    /**
     * Indicates if this audio channel is enabled.
     */
    public boolean isDisabled()
    {
        return mDisabled;
    }

    /**
     * Switches this channel's muted state
     * @param muted true to mute and false to unmute
     */
    public void setMuted(boolean muted)
    {
        mMuted = muted;
        notify(muted ? AudioEvent.Type.AUDIO_MUTED : AudioEvent.Type.AUDIO_UNMUTED);
    }

    /**
     * Indicates if this channel is muted
     * @return muted state
     */
    public boolean isMuted()
    {
        return mMuted;
    }

    /**
     * Plays the test audio sample data at higher priority that current audio queue.
     * @param samples of test audio to play.
     */
    public void playTest(float[] samples)
    {
        if(isDisabled())
        {
            return;
        }

        mAudioBuffer.insert(samples);
    }

    /**
     * Indicates if this channel has an audio segment, indicating that it is right in the middle of playback and
     * the audio output should temporarily pause until more audio is available from the segment.
     * @return true if this channel is processing a non-complete audio segment.
     */
    public boolean hasAudioSegment()
    {
        return mCurrentAudioSegment != null;
    }

    /**
     * Broadcasts a null identifier collection to the UI to clear any displayed metadata (TO/FROM) identifiers
     */
    public void clearMetadata()
    {
        if(mMetadataSent)
        {
            broadcast(null);
            mMetadataSent = false;
        }
    }

    /**
     * Provides audio for this channel from the audio segment queue.
     * @return audio or null if there are no audio segments or test audio to play.
     */
    public float[] getAudio()
    {
        if(mAudioBuffer.isFull())
        {
            float[] audio = mAudioBuffer.get();

            if(isMuted())
            {
                return new float[SAMPLES_PER_INTERVAL];
            }
            else
            {
                return audio;
            }
        }

        if(mCurrentAudioSegment == null)
        {
            loadNextAudioSegment();
        }

        //Evaluate current audio segment to see if the status has changed for duplicate or do-not-monitor.
        while(isThrowaway(mCurrentAudioSegment))
        {
            if(mCurrentBufferIndex > 0)
            {
                //Flush the buffer and insert the drop tone
                mAudioBuffer.replace(mAudioSegmentDropTone);
            }

            disposeCurrentAudioSegment();
            loadNextAudioSegment();
        }

        if(mCurrentAudioSegment != null)
        {
            mNoAudioFromSegmentIntervalCount++;
        }

        while(!mAudioBuffer.isFull() && mCurrentAudioSegment != null &&
                mCurrentBufferIndex < mCurrentAudioSegment.getAudioBufferCount())
        {
            if(mCurrentBufferIndex < 0)
            {
                mAudioBuffer.add(mAudioSegmentStartTone);
                mAudioBuffer.add(mCurrentAudioSegment.getAudioBuffer(0));
                mCurrentBufferIndex = 1;
                broadcast(mCurrentAudioSegment.getIdentifierCollection());
                mMetadataSent = true;
            }
            else
            {
                mAudioBuffer.add(mCurrentAudioSegment.getAudioBuffer(mCurrentBufferIndex++));
            }

            mNoAudioFromSegmentIntervalCount = 0;
        }

        //Dispose completed/fully-played audio segments and segments that have stalled/failed to progress
        if(mCurrentAudioSegment != null && ((mNoAudioFromSegmentIntervalCount >= 6) ||
          (mCurrentAudioSegment.isComplete() && (mCurrentBufferIndex >= mCurrentAudioSegment.getAudioBufferCount()))))
        {
            disposeCurrentAudioSegment();
            mNoAudioFromSegmentIntervalCount = 0;
        }

        float[] audio = null;

        if(mAudioBuffer.isFull())
        {
            audio = mAudioBuffer.get();
        }
        else if(mCurrentAudioSegment == null && !mAudioBuffer.isEmpty())
        {
            audio = mAudioBuffer.flush();
        }

        //Finally, if we have buffer audio and we're muted, return silence
        if(isMuted() && audio != null)
        {
            audio = new float[SAMPLES_PER_INTERVAL];
        }

        return audio;
    }

    /**
     * Notifies listener(s) of the audio event.
     * @param eventType to notify for this channel
     */
    public void notify(AudioEvent.Type eventType)
    {
        broadcastAudioEvent(new AudioEvent(eventType, getChannelName()));
    }
    /**
     * Registers a single listener to receive audio start and audio stop events
     */
    public void addAudioEventListener(Listener<AudioEvent> listener)
    {
        mAudioEventBroadcaster.addListener(listener);
    }

    /**
     * Removes the audio event listener
     * @param listener to remove
     */
    public void removeAudioEventListener(Listener<AudioEvent> listener)
    {
        mAudioEventBroadcaster.removeListener(listener);
    }

    /**
     * Broadcasts an audio event to the registered listener
     */
    private void broadcastAudioEvent(AudioEvent audioEvent)
    {
        mAudioEventBroadcaster.broadcast(audioEvent);
    }

    /**
     * Indicates if the audio segment should be thrown away because it is marked as duplicate or do-not-monitor.
     * @param audioSegment to evaluate
     * @return true if the audio should be thrown away.
     */
    private boolean isThrowaway(AudioSegment audioSegment)
    {
        return audioSegment != null && (audioSegment.isDoNotMonitor() || mDropDuplicates && (audioSegment.isDuplicate()));
    }

    /**
     * Channel name
     */
    public String getChannelName()
    {
        return mChannelName;
    }

    /**
     * Broadcasts audio identifier collection metadata to the registered listener
     */
    private void broadcast(IdentifierCollection identifierCollection)
    {
        if(mIdentifierCollectionListener != null)
        {
            mIdentifierCollectionListener.receive(identifierCollection);
        }
    }

    /**
     * Registers a single listener to receive the audio metadata from each
     * audio packet
     */
    public void setIdentifierCollectionListener(Listener<IdentifierCollection> listener)
    {
        mIdentifierCollectionListener = listener;
    }

    /**
     * Unregisters the current audio metadata listener
     */
    public void removeAudioMetadataListener()
    {
        mIdentifierCollectionListener = null;
    }

    /**
     * Prepares this audio output for disposal.
     */
    public void dispose()
    {

        MyEventBus.getGlobalEventBus().unregister(this);
        disposeCurrentAudioSegment();
        mAudioEventBroadcaster.clear();
        mIdentifierCollectionListener = null;
    }

    /**
     * Guava event bus notifications that the preferences have been updated, so that we can update audio segment tones.
     */
    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.PLAYBACK)
        {
            updateToneInsertionAudioClips();
        }
        else if(preferenceType == PreferenceType.DUPLICATE_CALL_DETECTION)
        {
            mDropDuplicates = mUserPreferences.getCallManagementPreference().isDuplicatePlaybackSuppressionEnabled();
        }
    }

    /**
     * Indicates if the audio segment is linked to the current playback audio segment
     */
    public boolean isLinkedTo(AudioSegment audioSegment)
    {
        return audioSegment.isLinked() && audioSegment.isLinkedTo(mCurrentAudioSegment);
    }

    /**
     * Receive and process audio identifier update notifications.
     */
    public void receive(IdentifierUpdateNotification identifierUpdateNotification)
    {
        if(mCurrentAudioSegment != null)
        {
            IdentifierCollection identifierCollection = mCurrentAudioSegment.getIdentifierCollection();

            if(identifierCollection != null)
            {
                broadcast(identifierCollection);
            }
        }
    }

    /**
     * Updates audio segment start, drop and preempt tone insertion clips
     */
    private void updateToneInsertionAudioClips()
    {
        mAudioSegmentStartTone = mUserPreferences.getPlaybackPreference().getStartTone();
        mAudioSegmentDropTone = mUserPreferences.getPlaybackPreference().getDropTone();;
    }

    /**
     * Indicates if this audio output doesn't currently have any audio segments queued for playback.
     * @return true if empty
     */
    public boolean isEmpty()
    {
        return mAudioSegmentQueue.isEmpty();
    }

    /**
     * Schedules the audio segment for playback.  The audio segment user count should already be incremented by the
     * calling entity.
     *
     * @param audioSegment to schedule for playback.
     */
    public void play(AudioSegment audioSegment)
    {
        if(audioSegment != null)
        {
            //Audio segment user count has already been incremented by the external caller.
            if(isDisabled())
            {
                audioSegment.decrementConsumerCount();
            }
            else
            {
                mAudioSegmentQueue.add(audioSegment);
            }
        }
    }

    /**
     * Dispose of the currently assigned audio segment.
     */
    private void disposeCurrentAudioSegment()
    {
        if(mCurrentAudioSegment != null)
        {
            mCurrentAudioSegment.decrementConsumerCount();
            mCurrentAudioSegment.removeIdentifierUpdateNotificationListener(this);
            mCurrentAudioSegment = null;
        }
    }

    /**
     * Loads the next audio segment from the queue.
     */
    private void loadNextAudioSegment()
    {
        AudioSegment audioSegment = mAudioSegmentQueue.poll();

        boolean verificationInProgress = (audioSegment != null);

        while(verificationInProgress)
        {
            if(audioSegment != null)
            {
                //Throw away the audio segment if it has been flagged as do not monitor or is duplicate
                if(isThrowaway(audioSegment))
                {
                    audioSegment.decrementConsumerCount();
                    audioSegment = mAudioSegmentQueue.poll();
                }
                else
                {
                    verificationInProgress = false;
                }
            }
            else
            {
                verificationInProgress = false;
            }
        }

        mCurrentAudioSegment = audioSegment;

        if(audioSegment != null)
        {
            mCurrentAudioSegment.addIdentifierUpdateNotificationListener(this);
            broadcast(mCurrentAudioSegment.getIdentifierCollection());
        }

        mCurrentBufferIndex = -1;
    }

    /**
     * Manages a stream of float PCM audio samples.  Employs thread locking of the audio buffer to support both the
     * audio processor thread and user initiated test audio replay.
     */
    static class AudioBuffer
    {
        private final Lock mLock = new ReentrantLock();
        private float[] mBuffer = new float[0];

        /**
         * Indicates if the buffer has at least one interval of samples available
         */
        public boolean isFull()
        {
            return mBuffer.length >= SAMPLES_PER_INTERVAL;
        }

        /**
         * Indicates if the buffer is empty
         */
        public boolean isEmpty()
        {
            return mBuffer.length == 0;
        }

        /**
         * Appends the audio to the buffer
         * @param audio to add
         */
        public void add(float[] audio)
        {
            if(audio != null)
            {
                mLock.lock();

                try
                {
                    mBuffer = concatenate(mBuffer, audio);
                }
                finally
                {
                    mLock.unlock();
                }
            }
        }

        /**
         * Inserts the audio to the front of the buffer.  This can be used to insert test audio ahead of any queued audio.
         * @param audio to insert
         */
        public void insert(float[] audio)
        {
            if(audio != null)
            {
                mLock.lock();
                try
                {
                    mBuffer = concatenate(audio, mBuffer);
                }
                finally
                {
                    mLock.unlock();
                }
            }
        }

        /**
         * Replaces the content in the buffer with the audio argument
         * @param audio to replace into the buffer
         */
        public void replace(float[] audio)
        {
            if(audio != null)
            {
                mLock.lock();

                try
                {
                    mBuffer = audio;
                }
                finally
                {
                    mLock.unlock();
                }
            }
        }

        /**
         * Concatenate two arrays.  Note: this does not null check either array.
         * @param a first array
         * @param b second array
         * @return concatenated array
         */
        private static float[] concatenate(float[] a, float[] b)
        {
            a = Arrays.copyOf(a, a.length + b.length);
            System.arraycopy(b, 0, a, a.length - b.length, b.length);
            return a;
        }

        /**
         * Retrieves an array of 160 samples if available, otherwise returns null.
         * @return 160 samples or null.
         */
        public float[] get()
        {
            if(mBuffer.length >= SAMPLES_PER_INTERVAL)
            {
                mLock.lock();
                try
                {
                    float[] fragment = Arrays.copyOf(mBuffer, SAMPLES_PER_INTERVAL);
                    mBuffer = Arrays.copyOfRange(mBuffer, SAMPLES_PER_INTERVAL, mBuffer.length);
                    return fragment;
                }
                finally
                {
                    mLock.unlock();
                }
            }

            return null;
        }

        /**
         * Flushes remaining samples from the buffer when the quantity is less than a full sample set.
         * @return remaining samples or null.
         */
        public float[] flush()
        {
            mLock.lock();

            try
            {
                if(mBuffer.length > 0)
                {
                    float[] fragment = Arrays.copyOf(mBuffer, SAMPLES_PER_INTERVAL);

                    if(mBuffer.length > SAMPLES_PER_INTERVAL)
                    {
                        mBuffer = Arrays.copyOfRange(mBuffer, SAMPLES_PER_INTERVAL, mBuffer.length);
                    }
                    else
                    {
                        mBuffer = new float[0];
                    }

                    return fragment;
                }

                return null;
            }
            finally
            {
                mLock.unlock();
            }
        }
    }
}
