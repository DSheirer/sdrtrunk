/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
import io.github.dsheirer.controller.NamingThreadFactory;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.log.LoggingSuppressor;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.mixer.MixerChannel;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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

/**
 * Audio output/playback channel for a single audio mixer channel.  Providers support for playback of audio segments
 * and broadcasts audio segment metadata to registered listeners (ie gui components).
 */
public abstract class AudioOutput implements LineListener, Listener<IdentifierUpdateNotification>
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioOutput.class);
    private static final LoggingSuppressor LOGGING_SUPPRESSOR = new LoggingSuppressor(mLog);
    private int mBufferStartThreshold;
    private int mBufferStopThreshold;
    private Listener<IdentifierCollection> mIdentifierCollectionListener;
    private Broadcaster<AudioEvent> mAudioEventBroadcaster = new Broadcaster<>();
    private SourceDataLine mOutput;
    private Mixer mMixer;
    private MixerChannel mMixerChannel;
    private FloatControl mGainControl;
    private BooleanControl mMuteControl;
    private AudioEvent mAudioStartEvent;
    private AudioEvent mAudioStopEvent;
    private boolean mCanProcessAudio = false;
    private LinkedTransferQueue<AudioSegment> mAudioSegmentQueue = new LinkedTransferQueue<>();
    private AudioSegment mCurrentAudioSegment;
    private int mCurrentBufferIndex = 0;
    private UserPreferences mUserPreferences;
    private ByteBuffer mAudioSegmentStartTone;
    private ByteBuffer mAudioSegmentDropTone;
    private boolean mRunning = false;
    private ScheduledExecutorService mScheduledExecutorService;
    private ScheduledFuture<?> mProcessorFuture;
    private boolean mDropDuplicates;
    private long mOutputLastTimestamp = 0;
    private static final long STALE_PLAYBACK_THRESHOLD_MS = 500;
    private AudioFormat mAudioFormat;
    private Line.Info mLineInfo;
    private int mRequestedBufferSize;

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
        mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory(
                "sdrtrunk audio output " + mixerChannel.name()));
        mUserPreferences = userPreferences;
        mDropDuplicates = mUserPreferences.getCallManagementPreference().isDuplicatePlaybackSuppressionEnabled();
        mAudioFormat = audioFormat;
        mLineInfo = lineInfo;
        mRequestedBufferSize = requestedBufferSize;

        try
        {
            mOutput = (SourceDataLine) mMixer.getLine(mLineInfo);

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
                        LOGGING_SUPPRESSOR.error("no gain control", 2, "Couldn't obtain " +
                            "MASTER GAIN control for stereo line [" + mixer.getMixerInfo().getName() + " | " +
                                getChannelName() + "]");
                    }

                    try
                    {
                        Control mute = mOutput.getControl(BooleanControl.Type.MUTE);
                        mMuteControl = (BooleanControl) mute;
                    }
                    catch(IllegalArgumentException iae)
                    {
                        LOGGING_SUPPRESSOR.error("no mute control", 2, "Couldn't obtain " +
                            "MUTE control for stereo line [" + mixer.getMixerInfo().getName() + " | " +
                            getChannelName() + "]");
                    }

					//Run the queue processor task every 100 milliseconds or 10 times a second
                    mProcessorFuture = mScheduledExecutorService.scheduleAtFixedRate(new AudioSegmentProcessor(),
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

        //Register to receive preference update notifications so we can update the preference items
        MyEventBus.getGlobalEventBus().register(this);
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
            //Audio segment use count has already been incremented by the external caller.
            mAudioSegmentQueue.add(audioSegment);
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
        else if(preferenceType == PreferenceType.DUPLICATE_CALL_DETECTION)
        {
            mDropDuplicates = mUserPreferences.getCallManagementPreference().isDuplicatePlaybackSuppressionEnabled();
        }
    }

    /**
     * Updates audio segment start, drop and preempt tone insertion clips
     */
    private void updateToneInsertionAudioClips()
    {
        mAudioSegmentStartTone = null;
        mAudioSegmentDropTone = null;

        float[] start = mUserPreferences.getPlaybackPreference().getStartTone();

        if(start != null)
        {
            mAudioSegmentStartTone = convert(start);
        }

        float[] drop = mUserPreferences.getPlaybackPreference().getDropTone();

        if(drop != null)
        {
            mAudioSegmentDropTone = convert(drop);
        }
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
        if(mOutput == null)
        {
            LOGGING_SUPPRESSOR.error("null output", 2, "Audio Output is null - ignoring audio playback request");
            return;
        }

        if(buffer != null && buffer.array().length > 0)
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
                mOutputLastTimestamp = System.currentTimeMillis();

                checkStart();

                //Something is causing the source data line to fail to accept audio byte data via the write() method
                //and this seems to have started around JDK22, maybe.  When this happens, close and then re-open the
                //data line to clear the error state.  Note: in testing this error condition, the line is showing
                //the buffer is empty and the capacity is fully available, so it should have accepted attempts to write
                //data, but it failed, as indicated by wrote=0.
                if(!mOutput.isRunning() && wrote <= 0)
                {
                    mOutput.close();

                    try
                    {
                        mOutput.open();
                    }
                    catch(Exception e)
                    {
                        mLog.error("Error after closing and attempting to reopen audio output", e);
                    }
                }
            }

            if(mOutput.isRunning() && wrote < buffer.array().length)
            {
                //This will block until the buffer is fully written to the data line
                mOutput.write(buffer.array(), wrote, buffer.array().length - wrote);
                mOutputLastTimestamp = System.currentTimeMillis();
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
            broadcast(null);
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

        mCurrentBufferIndex = 0;
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
     * Manage audio segment playback and process audio segment buffers.  This method is designed to be called
     * by a threaded processor repeatedly to playback the current audio segment and check for and start newly
     * added audio segments.  It also handles starting and stopping the playback source data line to avoid audio
     * discontinuities due to buffer underruns.
     */
    private void processAudio()
    {
        if(mCurrentAudioSegment == null)
        {
            loadNextAudioSegment();
        }

        //Evaluate current audio segment to see if the status has changed for duplicate or do-not-monitor.
        while(isThrowaway(mCurrentAudioSegment))
        {
            if(mCurrentBufferIndex > 0)
            {
                playAudio(mAudioSegmentDropTone);
            }

            disposeCurrentAudioSegment();
            loadNextAudioSegment();
        }

        while(mCurrentAudioSegment != null && mCurrentBufferIndex < mCurrentAudioSegment.getAudioBufferCount())
        {
            //Continuously evaluate current audio segment to see if the status has changed for duplicate or do-not-monitor.
            if(isThrowaway(mCurrentAudioSegment))
            {
                if(mCurrentBufferIndex > 0)
                {
                    playAudio(mAudioSegmentDropTone);
                }

                disposeCurrentAudioSegment();
            }
            else
            {
                if(mCurrentBufferIndex == 0)
                {
                    playAudio(mAudioSegmentStartTone);
                }

                try
                {
                    float[] audioBuffer = mCurrentAudioSegment.getAudioBuffers().get(mCurrentBufferIndex++);

                    if(audioBuffer != null)
                    {
                        ByteBuffer audio = convert(audioBuffer);
                        //This call blocks until all audio bytes are dumped into the data line.
                        playAudio(audio);
                    }
                }
                catch(Exception e)
                {
                    mLog.error("Error while processing audio for [" + mMixerChannel.name() + "]", e);
                }
            }
        }

        //Check for completed and fully-played audio segment to closeout
        if(mCurrentAudioSegment != null &&
           mCurrentAudioSegment.isComplete() &&
           (mCurrentBufferIndex >= mCurrentAudioSegment.getAudioBufferCount()))
        {
            disposeCurrentAudioSegment();
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
        disposeCurrentAudioSegment();
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
            if(mOutput.isRunning())
            {
                if(mOutput.available() >= mBufferStopThreshold)
                {
                    mOutput.drain();
                    mOutput.stop();
                    mRunning = false;
                }
                //Detect when we have a partially filled output that will neither start nor stop and the last write
                //timestamp was more than the staleness threshold (500 ms) -- stop the output and discard stale contents
                else if(mOutput.available() < mBufferStopThreshold &&
                        mOutput.available() > mBufferStartThreshold &&
                        (System.currentTimeMillis() - mOutputLastTimestamp >= STALE_PLAYBACK_THRESHOLD_MS))
                {
                    mOutput.stop();
                    //Discard the stale buffer contents.
                    mOutput.flush();
                    mRunning = false;
                }
            }
            //If output playback stopped on its own because the buffer emptied, then cleanup
            else
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
        private AtomicBoolean mProcessing = new AtomicBoolean();

        @Override
        public void run()
        {
            if(mProcessing.compareAndSet(false, true))
            {
                try
                {
                    processAudio();
                }
                catch(Throwable t)
                {
                    mLog.error("Error while processing audio buffers", t);
                }

                mProcessing.set(false);
            }
        }
    }
}
