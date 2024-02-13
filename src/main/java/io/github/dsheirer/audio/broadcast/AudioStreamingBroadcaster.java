/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.audio.broadcast;

import io.github.dsheirer.audio.convert.AudioFrames;
import io.github.dsheirer.audio.convert.ISilenceGenerator;
import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3FrameTools;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.util.ThreadPool;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AudioStreamingBroadcaster<T extends BroadcastConfiguration> extends AbstractAudioBroadcaster<T>
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioStreamingBroadcaster.class);

    public static final int PROCESSOR_RUN_INTERVAL_MS = 1000;
    private ScheduledFuture<?> mRecordingQueueProcessorFuture;

    private RecordingQueueProcessor mRecordingQueueProcessor = new RecordingQueueProcessor();
    private Queue<AudioRecording> mAudioRecordingQueue = new LinkedTransferQueue<>();
    private ISilenceGenerator mSilenceGenerator;

    private BroadcastFormat mBroadcastFormat;
    private long mDelay;
    private long mMaximumRecordingAge;
    private AtomicBoolean mStreaming = new AtomicBoolean();

    protected boolean mInlineActive = false;
    protected int mInlineInterval;
    protected int mInlineRemaining = -1;

    private int mTimeOverrun = 0;

    /**
     * AudioBroadcaster for streaming audio recordings to a remote streaming audio server.  Audio recordings are
     * generated by an internal StreamManager that converts an inbound stream of AudioPackets into a recording of the
     * desired audio format (e.g. MP3) and nominates the recording to an internal recording queue for streaming.  The
     * broadcaster supports receiving audio packets from multiple audio sources.  Each audio packet's internal audio
     * metadata source string is used to reassemble each packet stream.  Recordings are capped at 30 seconds length.
     * If a source audio packet stream exceeds 30 seconds in length, it will be chunked into 30 second recordings.
     *
     * This broadcaster supports a time delay setting for delaying broadcast of audio recordings.  The delay setting is
     * defined in the broadcast configuration.  When this delay is greater than zero, the recording will remain in the
     * audio broadcaster queue until the recording start time + delay elapses.  Audio recordings are processed in a FIFO
     * manner.
     *
     * Use the start() and stop() methods to connect to/disconnect from the remote server.  Audio recordings will be
     * streamed to the remote server when available.  One second silence frames will be broadcast to the server when
     * there are no recordings available, in order to maintain a connection with the remote server.  Any audio packet
     * streams received while the broadcaster is stopped will be ignored.
     *
     * The last audio packet's metadata is automatically attached to the closed audio recording when it is enqueued for
     * broadcast.  That metadata will be updated on the remote server once the audio recording is opened for streaming.
     */
    public AudioStreamingBroadcaster(T broadcastConfiguration, InputAudioFormat inputAudioFormat, MP3Setting mp3Setting)
    {
        super(broadcastConfiguration);
        mBroadcastFormat = broadcastConfiguration.getBroadcastFormat();
        mDelay = getBroadcastConfiguration().getDelay();
        mMaximumRecordingAge = getBroadcastConfiguration().getMaximumRecordingAge();
        mSilenceGenerator = BroadcastFactory.getSilenceGenerator(broadcastConfiguration.getBroadcastFormat(),
        inputAudioFormat, mp3Setting);
    }

    public void dispose()
    {
    }

    /**
     * Broadcast binary audio data frames or sequences.
     */
    protected abstract void broadcastAudio(byte[] audio, IdentifierCollection identifierCollection, long time);

    /**
     * Protocol-specific metadata updater
     */
    protected abstract IBroadcastMetadataUpdater getMetadataUpdater();

    /**
     * Broadcasts the next song's audio metadata prior to streaming the next song.
     *
     * @param identifierCollection for the next recording that will be streamed
     * @param startTime containing recording start time as MS since epoch
     */
    protected void broadcastMetadata(IdentifierCollection identifierCollection, long startTime)
    {
        IBroadcastMetadataUpdater metadataUpdater = getMetadataUpdater();

        if(metadataUpdater != null)
        {
            metadataUpdater.update(identifierCollection, startTime);
        }
    }

    /**
     * Disconnects the broadcaster from the remote server for a reset or final stop.
     */
    protected abstract void disconnect();

    /**
     * Connects to the remote server specified by the broadcast configuration and starts audio streaming.
     */
    public void start()
    {
        if(mStreaming.compareAndSet(false, true))
        {
            if(mRecordingQueueProcessorFuture == null)
            {
                mRecordingQueueProcessorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(mRecordingQueueProcessor,
                    0, PROCESSOR_RUN_INTERVAL_MS, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Disconnects from the remote server.
     */
    public void stop()
    {
        if(mStreaming.compareAndSet(true, false))
        {
            if(mRecordingQueueProcessorFuture != null)
            {
                mRecordingQueueProcessorFuture.cancel(true);
                mRecordingQueueProcessorFuture = null;
            }

            disconnect();
        }
    }

    /**
     * Stream name for the broadcast configuration for this broadcaster
     *
     * @return stream name or null
     */
    public String getStreamName()
    {
        BroadcastConfiguration config = getBroadcastConfiguration();

        if(config != null)
        {
            return config.getName();
        }

        return null;
    }

    /**
     * Size of recording queue for recordings awaiting streaming
     */
    public int getAudioQueueSize()
    {
        return mAudioRecordingQueue.size();
    }

    /**
     * Primary insert method for the stream manager to nominate completed audio recordings for broadcast.
     *
     * @param recording to queue for broadcasting
     */
    public void receive(AudioRecording recording)
    {
        if(connected())
        {
            mAudioRecordingQueue.offer(recording);
            broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_QUEUE_CHANGE));
        }
        else
        {
            recording.removePendingReplay();
        }
    }

    /**
     * Sets the state of the broadcastAudio connection
     */
    public void setBroadcastState(BroadcastState state)
    {
        if(mBroadcastState.get() != state)
        {
            if(state == BroadcastState.CONNECTED || state == BroadcastState.DISCONNECTED)
            {
                mLog.info("[" + getStreamName() + "] status: " + state);
            }

            super.setBroadcastState(state);

            if(mBroadcastState.get() != null && mBroadcastState.get().isErrorState())
            {
                stop();
            }

            if(!connected())
            {
                //Reset inline metadata
                mInlineRemaining = -1;
                //Remove all pending audio recordings
                while(!mAudioRecordingQueue.isEmpty())
                {
                    try
                    {
                        AudioRecording recording = mAudioRecordingQueue.remove();
                        recording.removePendingReplay();
                    }
                    catch(Exception e)
                    {
                        //Ignore
                    }
                }
            }
        }
    }

    /**
     * Indicates if the broadcaster is currently connected to the remote server
     */
    protected boolean connected()
    {
        return getBroadcastState() == BroadcastState.CONNECTED;
    }

    /**
     * Indicates if this broadcaster can connect and is not currently in an error state or a connected state.
     */
    public boolean canConnect()
    {
        BroadcastState state = getBroadcastState();
        return state != BroadcastState.CONNECTED && !state.isErrorState();
    }

    /**
     * Indicates if the current broadcast state is an error state, meaning that it cannot recover or connect using the
     * current configuration.
     */
    protected boolean isErrorState()
    {
        return getBroadcastState().isErrorState();
    }


    /**
     * Audio recording queue processor.  Fetches recordings from the queue and chunks the recording byte content
     * to subclass implementations for broadcast in the appropriate manner.
     */
    public class RecordingQueueProcessor implements Runnable
    {
        private AtomicBoolean mProcessing = new AtomicBoolean();
        private AudioFrames mInputFrames;
        private IdentifierCollection mInputIdentifierCollection;
        private long mInputStart = -1;

        @Override
        public void run()
        {
            if(mProcessing.compareAndSet(false, true))
            {
                try
                {
                    int timeSent = 0;

                    if(mInputFrames == null || !mInputFrames.hasNextFrame())
                    {
                        nextRecording();
                    }

                    if(mInputFrames != null && mInputFrames.hasNextFrame())
                    {
                        AudioFrames segment = mInputFrames.getSegment(PROCESSOR_RUN_INTERVAL_MS);
                        broadcastAudio(segment.toByteArray(), mInputIdentifierCollection, mInputStart + mInputFrames.getCurrentPositionTime());
                        timeSent += segment.getDuration();
                    }

                    if((mInputFrames == null || !mInputFrames.hasNextFrame()) && timeSent < PROCESSOR_RUN_INTERVAL_MS)
                    {
                        AudioFrames silenceFrames = mSilenceGenerator.generate(PROCESSOR_RUN_INTERVAL_MS - mTimeOverrun - timeSent);
                        broadcastAudio(silenceFrames.toByteArray(), null, -1);
                        timeSent += silenceFrames.getDuration();
                }

                    mTimeOverrun += timeSent - PROCESSOR_RUN_INTERVAL_MS;
                }
                catch(Throwable t)
                {
                    mLog.error("Error while processing audio streaming queue", t);
                }

                mProcessing.set(false);
            }
        }

        /**
         * Loads the next recording for broadcast
         */
        private void nextRecording()
        {
            boolean metadataUpdateRequired = false;

            if(mInputFrames != null)
            {
                mStreamedAudioCount++;
                broadcast(new BroadcastEvent(AudioStreamingBroadcaster.this,
                    BroadcastEvent.Event.BROADCASTER_STREAMED_COUNT_CHANGE));
                metadataUpdateRequired = true;
            }

            mInputFrames = null;
            mInputIdentifierCollection = null;
            mInputStart = -1;

            //Peek at the next recording but don't remove it from the queue yet, so we can inspect the start time for
            //age limits and/or delay elapsed
            AudioRecording nextRecording = mAudioRecordingQueue.peek();

            //Purge any recordings that have exceeded maximum recording age limit
            while(nextRecording != null &&
                (nextRecording.getStartTime() + mDelay + mMaximumRecordingAge) < java.lang.System.currentTimeMillis())
            {
                nextRecording = mAudioRecordingQueue.remove();
                nextRecording.removePendingReplay();
                mAgedOffAudioCount++;
                broadcast(new BroadcastEvent(AudioStreamingBroadcaster.this,
                    BroadcastEvent.Event.BROADCASTER_AGED_OFF_COUNT_CHANGE));
                nextRecording = mAudioRecordingQueue.peek();
            }

            if(nextRecording != null && nextRecording.getStartTime() + mDelay <= System.currentTimeMillis())
            {
                nextRecording = mAudioRecordingQueue.remove();

                try
                {
                    if(Files.exists(nextRecording.getPath()))
                    {
                        byte[] audio = Files.readAllBytes(nextRecording.getPath());

                        if(audio.length > 0)
                        {
                            switch(mBroadcastFormat)
                            {
                                case MP3:
                                    mInputFrames = MP3FrameTools.split(audio);
                                    break;
                                default:
                                    throw new IllegalArgumentException("Unsupported broadcast format [" + mBroadcastFormat + "]");
                            }
                            mInputIdentifierCollection = nextRecording.getIdentifierCollection();
                            mInputStart = nextRecording.getStartTime();

                            if(connected())
                            {
                                broadcastMetadata(nextRecording.getIdentifierCollection(), mInputStart);
                            }

                            metadataUpdateRequired = false;
                        }
                    }
                }
                catch(IOException ioe)
                {
                    mLog.error("Stream [" + getBroadcastConfiguration().getName() + "] error reading temporary audio " +
                        "stream recording [" + nextRecording.getPath().toString() + "] - skipping recording - ", ioe);

                    mInputFrames = null;
                    mInputIdentifierCollection = null;
                    mInputStart = -1;
                    metadataUpdateRequired = false;
                }

                nextRecording.removePendingReplay();

                broadcast(new BroadcastEvent(AudioStreamingBroadcaster.this, BroadcastEvent.Event.BROADCASTER_QUEUE_CHANGE));
            }

            //If we closed out a recording and don't have a new/next recording, send an empty metadata update
            if(metadataUpdateRequired && connected())
            {
                broadcastMetadata(null, -1);
            }
        }
    }
}
