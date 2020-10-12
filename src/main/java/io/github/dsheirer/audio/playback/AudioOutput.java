/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.audio.playback;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.alias.id.priority.Priority;
import io.github.dsheirer.audio.AudioEvent;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.mixer.MixerChannel;
import io.github.dsheirer.util.ThreadPool;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Audio output/playback channel for a single audio mixer channel.  Providers support for playback of audio segments
 * and broadcasts audio segment metadata to registered listeners (ie gui components).
 */
public abstract class AudioOutput implements LineListener, Listener<IdentifierUpdateNotification>
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioOutput.class);
    private int mBufferStartThreshold;
    private int mBufferStopThreshold;
    private Listener<IdentifierCollection> mIdentifierCollectionListener;
    private Broadcaster<AudioEvent> mAudioEventBroadcaster = new Broadcaster<>();
    private ScheduledFuture<?> mProcessorFuture;
    private SourceDataLine mOutput;
    private Mixer mMixer;
    private MixerChannel mMixerChannel;
    private FloatControl mGainControl;
    private BooleanControl mMuteControl;
    private AudioEvent mAudioStartEvent;
    private AudioEvent mAudioStopEvent;
    private boolean mCanProcessAudio = false;
    private AudioSegment mCurrentAudioSegment;
    private AudioSegment mNextAudioSegment;
    private ReentrantLock mLock = new ReentrantLock();
    private int mCurrentBufferIndex = 0;
    private UserPreferences mUserPreferences;
    private BooleanProperty mEmptyProperty = new SimpleBooleanProperty(true);
    private IntegerProperty mAudioPriority = new SimpleIntegerProperty(Priority.DEFAULT_PRIORITY);
    private ByteBuffer mAudioSegmentStartTone;
    private ByteBuffer mAudioSegmentPreemptTone;
    private ByteBuffer mAudioSegmentDropTone;
    private boolean mRunning = false;

    /**
     * Single audio channel playback with automatic starting and stopping of the
     * underlying sourcedataline specified by the mixer and mixer channel
     * arguments.
     *
     * Maintains an internal non-blocking audio packet queue and processes this
     * queue 25 times a second (every 40 ms).
     *
     * @param mixer to obtain source data line
     * @param mixerChannel either mono or left/right stereo
     * @param audioFormat to use during playback
     * @param lineInfo to use when obtaining the source data line
     * @param requestedBufferSize of approximately 1 second of audio
     */
    public AudioOutput(Mixer mixer, MixerChannel mixerChannel, AudioFormat audioFormat, Line.Info lineInfo,
                       int requestedBufferSize, UserPreferences userPreferences)
    {
        mMixer = mixer;
        mMixerChannel = mixerChannel;
        mUserPreferences = userPreferences;

        try
        {
            mOutput = (SourceDataLine) mMixer.getLine(lineInfo);

            if(mOutput != null)
            {
                mOutput.open(audioFormat, requestedBufferSize);

                //Start threshold: buffer is full with 10% or less of capacity remaining
                mBufferStartThreshold = (int) (mOutput.getBufferSize() * 0.10);

                //Stop threshold: buffer is empty with 90% or more capacity available
                mBufferStopThreshold = (int) (mOutput.getBufferSize() * 0.90);

                mOutput.addLineListener(this);

                if(mOutput != null)
                {
                    try
                    {
                        Control gain = mOutput.getControl(FloatControl.Type.MASTER_GAIN);
                        mGainControl = (FloatControl) gain;
                    }
                    catch(IllegalArgumentException iae)
                    {
                        mLog.warn("Couldn't obtain MASTER GAIN control for stereo line [" +
                            mixer.getMixerInfo().getName() + " | " + getChannelName() + "]");
                    }

                    try
                    {
                        Control mute = mOutput.getControl(BooleanControl.Type.MUTE);
                        mMuteControl = (BooleanControl) mute;
                    }
                    catch(IllegalArgumentException iae)
                    {
                        mLog.warn("Couldn't obtain MUTE control for stereo line [" +
                            mixer.getMixerInfo().getName() + " | " + getChannelName() + "]");
                    }

					//Run the queue processor task every 100 milliseconds or 10 times a second
                    mProcessorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(new AudioSegmentProcessor(),
                        0, 100, TimeUnit.MILLISECONDS);
                }

                mAudioStartEvent = new AudioEvent(AudioEvent.Type.AUDIO_STARTED, getChannelName());
                mAudioStopEvent = new AudioEvent(AudioEvent.Type.AUDIO_STOPPED, getChannelName());
                mCanProcessAudio = true;
            }
        }
        catch(LineUnavailableException e)
        {
            mLog.error("Couldn't obtain audio source data line for audio output - mixer [" +
                mMixer.getMixerInfo().getName() + "]");
        }

        updateToneInsertionAudioClips();

        //Register to receive directory preference update notifications so we can update the preference items
        MyEventBus.getGlobalEventBus().register(this);
    }

    /**
     * Boolean property that indicates if this audio output has an audio segment queued or in process of playback.
     */
    public BooleanProperty emptyProperty()
    {
        return mEmptyProperty;
    }

    /**
     * Audio playback priority of the current audio segment or default audio priority if nothing is currently loaded.
     */
    public IntegerProperty audioPriorityProperty()
    {
        return mAudioPriority;
    }

    /**
     * Schedules the audio segment for playback.
     *
     * Note: if a segment is currently playing and another audio segment is already queued for playback, invoking
     * this method will overwrite the queued segment with the argument.
     *
     * @param audioSegment to schedule for playback.
     */
    public void play(AudioSegment audioSegment)
    {
        if(audioSegment != null)
        {
            mLock.lock();

            if(mNextAudioSegment != null)
            {
                mNextAudioSegment.decrementConsumerCount();
                mNextAudioSegment = null;
            }

            try
            {
                mNextAudioSegment = audioSegment;
                mEmptyProperty.set(false);
            }
            finally
            {
                mLock.unlock();
            }
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
    @Override
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
     * Guava event bus notifications that the preferences have been updated, so that we can update audio segment tones.
     */
    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.PLAYBACK)
        {
            updateToneInsertionAudioClips();
        }
    }

    /**
     * Updates audio segment start, drop and preempt tone insertion clips
     */
    private void updateToneInsertionAudioClips()
    {
        mAudioSegmentStartTone = null;
        mAudioSegmentPreemptTone = null;
        mAudioSegmentDropTone = null;

        float[] start = mUserPreferences.getPlaybackPreference().getStartTone();

        if(start != null)
        {
            mAudioSegmentStartTone = convert(start);
        }

        float[] preempt = mUserPreferences.getPlaybackPreference().getPreemptTone();

        if(preempt != null)
        {
            mAudioSegmentPreemptTone = convert(preempt);
        }

        float[] drop = mUserPreferences.getPlaybackPreference().getDropTone();

        if(drop != null)
        {
            mAudioSegmentDropTone = convert(drop);
        }
    }

    /**
     * Disposes of a processed audio segment and notifies the originator that processing of the segment is complete
     * by decrementing the consumer count.
     * @param audioSegment to dispose
     */
    private void dispose(AudioSegment audioSegment)
    {
        mAudioPriority.unbind();

        if(audioSegment != null)
        {
            audioSegment.decrementConsumerCount();
            audioSegment.removeIdentifierUpdateNotificationListener(this);
        }
    }

    /**
     * Generates a tone indicating that a new audio segment is starting
     */
    private ByteBuffer getAudioSegmentStartTone()
    {
        return mAudioSegmentStartTone;
    }

    /**
     * Generates a tone indicating that the current audio segment playback has been preempted for a higher priority
     * audio segment that is now starting.
     */
    private ByteBuffer getAudioSegmentPreemptionTone()
    {
        return mAudioSegmentPreemptTone;
    }

    /**
     * Generates a tone indicating that the current audio segment playback has been dropped because the audio segment
     * has been flagged as Do Not Monitor after playback has started.
     */
    private ByteBuffer getAudioSegmentDropTone()
    {
        return mAudioSegmentDropTone;
    }

    /**
     * Writes the audio buffer data to the source data line.  If the data line is not currently playing, write as
     * much of the buffer to the data line as will fit, start the dataline, and then finish writing the residual
     * buffer content to the data line as a blocking call.
     *
     * @param buffer of audio to playback
     */
    private void playAudio(ByteBuffer buffer)
    {
        if(buffer != null)
        {
            int wrote = 0;

            //If the output data line is not running, we can only write up to the available capacity.  So, only write
            //what will fit initially, start playback, and then use a blocking write for the remainder.
            if(!mOutput.isRunning())
            {
                int toWrite = mOutput.available();

                if(toWrite > buffer.array().length)
                {
                    toWrite = buffer.array().length;
                }

                //Top off the buffer and check if we can start it
                wrote += mOutput.write(buffer.array(), 0, toWrite);

                checkStart();
            }

            if(mOutput.isRunning() && wrote < buffer.array().length)
            {
                //This will block until the buffer is fully written to the data line
                mOutput.write(buffer.array(), wrote, buffer.array().length - wrote);
            }
        }
    }

    /**
     * Manage audio segment playback and process audio segment buffers.  This method is designed to be called
     * by a threaded processor repeatedly to playback the current audio segment and check for and start a newly
     * assigned audio segment.  It also handles starting and stopping the playback source data line to avoid audio
     * discontinuities due to buffer underruns.
     */
    private void processAudio()
    {
        if(mNextAudioSegment != null)
        {
            mLock.lock();

            try
            {
                //Remove an assigned segment that's subsequently flagged as duplicate or do not monitor before playback
                if(mNextAudioSegment.isDoNotMonitor() || (mNextAudioSegment.isDuplicate() &&
                    mUserPreferences.getDuplicateCallDetectionPreference().isDuplicatePlaybackSuppressionEnabled()))
                {
                    mNextAudioSegment = null;

                    if(mCurrentAudioSegment == null)
                    {
                        mEmptyProperty.set(true);
                    }
                }
                //For linked audio segments, allow the linked segment to complete first before assigning the next
                else if(mNextAudioSegment.isLinked())
                {
                    if(mCurrentAudioSegment == null)
                    {
                        mCurrentAudioSegment = mNextAudioSegment;
                        mNextAudioSegment = null;
                        mCurrentBufferIndex = 0;

                        if(mCurrentAudioSegment != null)
                        {
                            mAudioPriority.bind(mCurrentAudioSegment.monitorPriorityProperty());
                            mCurrentAudioSegment.addIdentifierUpdateNotificationListener(this);
                            broadcast(mCurrentAudioSegment.getIdentifierCollection());
                        }
                        else
                        {
                            mAudioPriority.setValue(Priority.DEFAULT_PRIORITY);
                        }
                    }
                }
                else
                {
                    //Insert audio segment start or audio priority preemption bonk tone
                    if(mCurrentAudioSegment == null)
                    {
                        playAudio(getAudioSegmentStartTone());
                    }
                    else if(mCurrentBufferIndex > 0 &&
                        (!mCurrentAudioSegment.completeProperty().get() ||
                            mCurrentBufferIndex < mCurrentAudioSegment.getAudioBufferCount()))
                    {
                        playAudio(getAudioSegmentPreemptionTone());
                    }
                    else
                    {
                        playAudio(getAudioSegmentStartTone());
                    }

                    //Close current audio segment
                    dispose(mCurrentAudioSegment);
                    mCurrentAudioSegment = mNextAudioSegment;
                    mNextAudioSegment = null;
                    mCurrentBufferIndex = 0;

                    if(mCurrentAudioSegment != null)
                    {
                        mAudioPriority.bind(mCurrentAudioSegment.monitorPriorityProperty());
                        mCurrentAudioSegment.addIdentifierUpdateNotificationListener(this);
                        broadcast(mCurrentAudioSegment.getIdentifierCollection());
                    }
                    else
                    {
                        mAudioPriority.setValue(Priority.DEFAULT_PRIORITY);
                    }
                }
            }
            finally
            {
                mLock.unlock();
            }
        }

        if(mCurrentAudioSegment != null)
        {
            //Check for completed audio segment or a segment flagged as duplicate or Do Not Monitor
            if(mCurrentAudioSegment.isDoNotMonitor() ||
               (mCurrentAudioSegment.isDuplicate() &&
                mUserPreferences.getDuplicateCallDetectionPreference().isDuplicatePlaybackSuppressionEnabled()) ||
                (mCurrentAudioSegment.completeProperty().get() && mCurrentBufferIndex >= mCurrentAudioSegment.getAudioBufferCount()))
            {
                if(mCurrentAudioSegment.isDoNotMonitor())
                {
                    playAudio(getAudioSegmentDropTone());
                }

                dispose(mCurrentAudioSegment);
                mCurrentAudioSegment = null;

                mLock.lock();

                try
                {
                    if(mNextAudioSegment == null)
                    {
                        mEmptyProperty.set(true);
                    }
                }
                finally
                {
                    mLock.unlock();
                }

                return;
            }

            //Process any new buffers that have been added to the audio segment.  If a next audio segment gets assigned
            //while processing, exit the loop so that we can evaluate the next for higher priority preempt.  If the next
            //segment is a linked segment, ignore it so that we can close out the current segment.
            while(mCurrentAudioSegment != null && (mNextAudioSegment == null || mNextAudioSegment.isLinked()) &&
                   mCurrentBufferIndex < mCurrentAudioSegment.getAudioBufferCount() &&
                   !mCurrentAudioSegment.isDoNotMonitor() && !(mCurrentAudioSegment.isDuplicate() &&
                mUserPreferences.getDuplicateCallDetectionPreference().isDuplicatePlaybackSuppressionEnabled()))
            {
                float[] audioBuffer = mCurrentAudioSegment.getAudioBuffers().get(mCurrentBufferIndex++);

                if(audioBuffer != null)
                {
                    ByteBuffer audio = convert(audioBuffer);
                    playAudio(audio);
                }
            }
        }

        checkStop();
    }

    /**
     * Prepares this audio output for disposal.
     */
    public void dispose()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
        mCanProcessAudio = false;

        if(mProcessorFuture != null)
        {
            mProcessorFuture.cancel(true);
        }

        mProcessorFuture = null;

        mLock.lock();

        try
        {
            if(mNextAudioSegment != null)
            {
                mNextAudioSegment.decrementConsumerCount();
                mNextAudioSegment = null;
            }
        }
        finally
        {
            mLock.unlock();
        }

        dispose(mCurrentAudioSegment);
        mCurrentAudioSegment = null;

        mAudioEventBroadcaster.clear();
        mIdentifierCollectionListener = null;

        if(mOutput != null)
        {
            mOutput.close();
        }

        mOutput = null;
        mGainControl = null;
        mMuteControl = null;
    }

    /**
     * Converts the audio buffer data into a byte buffer format appropriate for the underlying source data line.
     */
    protected abstract ByteBuffer convert(float[] buffer);

    /**
     * Audio output channel name
     */
    public String getChannelName()
    {
        return mMixerChannel.getLabel();
    }

    /**
     * Mixer Channel for this audio output
     */
    protected MixerChannel getMixerChannel()
    {
        return mMixerChannel;
    }

    /**
     * Registers a single listener to receive audio start and audio stop events
     */
    public void addAudioEventListener(Listener<AudioEvent> listener)
    {
        mAudioEventBroadcaster.addListener(listener);
    }

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
     * Starts audio playback once audio buffer is almost full and remaining capacity falls below the start threshold.
     *
     * Note: this method should only be invoked from the processAudio() method
     */
    private void checkStart()
    {
        if(mCanProcessAudio && !mOutput.isRunning() && mOutput.available() <= mBufferStartThreshold)
        {
            mOutput.start();
            mRunning = true;
        }
    }

    /**
     * Stops audio playback and drains the audio buffer to empty when the audio buffer is mostly empty and the
     * available buffer capacity exceeds the stop threshold
     *
     * Note: this method should only be invoked from the processAudio() method
     */
    private void checkStop()
    {
        if(mRunning)
        {
            //If the output buffer falls below the threshold then drain the output and stop playback
            if(mOutput.isRunning() && mOutput.available() >= mBufferStopThreshold)
            {
                mOutput.drain();
                mOutput.stop();
                mRunning = false;
            }
            //If output playback stopped on its own because the buffer emptied, then cleanup
            else if(!mOutput.isRunning())
            {
                mRunning = false;
            }

            //If we stopped audio playback, broadcast a null identifier to clear the gui panel
            if(!mRunning)
            {
                broadcast(null);
            }
        }
    }

    /**
     * Sets the mute state for this audio output channel
     */
    public void setMuted(boolean muted)
    {
        if(mMuteControl != null)
        {
            mMuteControl.setValue(muted);
            broadcastAudioEvent(new AudioEvent(muted ? AudioEvent.Type.AUDIO_MUTED : AudioEvent.Type.AUDIO_UNMUTED, getChannelName()));
        }
    }

    /**
     * Current mute state for this audio output channel
     */
    public boolean isMuted()
    {
        if(mMuteControl != null)
        {
            return mMuteControl.getValue();
        }

        return false;
    }

    /**
     * Gain/volume control for this audio output channel, if one is available.
     */
    public FloatControl getGainControl()
    {
        return mGainControl;
    }

    /**
     * Indicates if this audio output has a gain control available
     */
    public boolean hasGainControl()
    {
        return mGainControl != null;
    }

    /**
     * Monitors the source data line playback state and broadcasts audio events to the registered listener as the
     * state changes
     */
    @Override
    public void update(LineEvent event)
    {
        LineEvent.Type type = event.getType();

        if(type == LineEvent.Type.START)
        {
            mAudioEventBroadcaster.broadcast(mAudioStartEvent);
        }
        else if(type == LineEvent.Type.STOP)
        {
            mAudioEventBroadcaster.broadcast(mAudioStopEvent);
        }
    }

    /**
     * Runnable audio segment processor
     */
    public class AudioSegmentProcessor implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                processAudio();
            }
            catch(Throwable t)
            {
                mLog.error("Error while processing audio buffers", t);
            }
        }
    }
}
