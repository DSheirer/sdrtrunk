/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
 *
 ******************************************************************************/
package audio.broadcast;

import audio.AudioPacket;
import audio.IAudioPacketListener;
import audio.metadata.AudioMetadata;
import controller.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import properties.SystemProperties;
import sample.Listener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AudioBroadcaster implements IAudioPacketListener, Listener<AudioPacket>
{
    private final static Logger mLog = LoggerFactory.getLogger( AudioBroadcaster.class );
    public static final int PROCESSOR_RUN_INTERVAL_MS = 1000;

    private ThreadPoolManager mThreadPoolManager;
    private ScheduledFuture mScheduledTask;

    private StreamManager mStreamManager;
    private RecordingQueueProcessor mRecordingQueueProcessor = new RecordingQueueProcessor();
    private ArrayBlockingQueue<StreamableAudioRecording> mAudioRecordingQueue;
    private byte[] mSilenceFrame;
    private int mStreamedAudioCount = 0;

    private Listener<BroadcastEvent> mBroadcastEventListener;
    private BroadcastState mBroadcastState = BroadcastState.READY;

    private BroadcastConfiguration mBroadcastConfiguration;

    private boolean mEnabled = true;

    /**
     * AudioBroadcaster for sending audio to a remote broadcastAudio server.
     */
    public AudioBroadcaster(ThreadPoolManager threadPoolManager, BroadcastConfiguration broadcastConfiguration)
    {
        mThreadPoolManager = threadPoolManager;
        mBroadcastConfiguration = broadcastConfiguration;

        //Recording queue - bounded at 10 queued recordings maximum - new stream recordings will be deleted once full
        mAudioRecordingQueue = new ArrayBlockingQueue<>(10);

        //Create a 1 second silence frame - from 1200 millis of silence
        mSilenceFrame = BroadcastFactory.getSilenceFrame(getBroadcastConfiguration().getBroadcastFormat(), 1200);

        mStreamManager = new StreamManager(threadPoolManager, this, broadcastConfiguration.getDelay(),
                SystemProperties.getInstance().getApplicationFolder(BroadcastModel.TEMPORARY_STREAM_DIRECTORY));

        mStreamManager.start();

        startProcessor();
    }

    /**
     * Recording backlog awaiting streaming
     */
    public int getQueueSize()
    {
        return mAudioRecordingQueue.size();
    }

    /**
     * Number of audio recordings streamed to remote server
     */
    public int getStreamedAudioCount()
    {
        return mStreamedAudioCount;
    }

    /**
     * Queues the streamable audio recording for broadcastAudio.  The bounded queue is managed by removing older recordings
     * at the head of the queue, once full, to make room for new recordings.  This ensures that once the broadcaster
     * (re)starts broadcasting or catches up with the queue, that it is broadcasting the most current audio recordings.
     *
     * @param recording to queue for broadcasting
     */
    public void receive(StreamableAudioRecording recording)
    {
        boolean success = false;

        try
        {
            success = mAudioRecordingQueue.add(recording);
        }
        catch(IllegalStateException ise)
        {
            //The queue is full - delete the oldest recording at the head of the queue to make room for a new one
            removeRecording(mAudioRecordingQueue.poll());
        }

        if(!success)
        {
            try
            {
                success = mAudioRecordingQueue.add(recording);
            }
            catch(IllegalStateException ise)
            {
                //Do nothing on second attempt
            }
        }

        //If we can't queue the recording after two attempts, remove the recording
        if(!success)
        {
            removeRecording(recording);
        }
        else
        {
            broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_QUEUE_CHANGE));
        }
    }

    /**
     * Removes the temporary recording file from disk.
     *
     * @param recording to remove
     */
    private void removeRecording(StreamableAudioRecording recording)
    {
        try
        {
            Files.delete(recording.getPath());
        }
        catch(IOException ioe)
        {
            mLog.error("Error deleting temporary internet recording file: " + recording.getPath().toString());
        }
    }

    /**
     * Broadcast configuration used by this broadcaster
     */
    public BroadcastConfiguration getBroadcastConfiguration()
    {
        return mBroadcastConfiguration;
    }

    /**
     * IAudioPacketListener interface method
     */
    @Override
    public Listener<AudioPacket> getAudioPacketListener()
    {
        return this;
    }

    /**
     * Broadcasts the audio binary data.
     */
    protected abstract void broadcastAudio(byte[] audio);

    /**
     * Broadcasts the next song's audio metadata prior to streaming the next song.
     * @param metadata
     */
    protected abstract void broadcastMetadata(AudioMetadata metadata);

    /**
     * Disconnects the broadcaster and halts all broadcasting.
     */
    public void stop()
    {
        mEnabled = false;
        mStreamManager.stop();
        stopProcessor();
    }

    /**
     * Primary audio packet input method.  Audio packets are queued and then processed by the underlying broadcastAudio
     * handler for broadcastAudio to the remote server.
     */
    public void receive(AudioPacket packet)
    {
        if(mEnabled)
        {
            mStreamManager.receive(packet);
        }
    }

    /**
     * Starts the audio queue processor.  The audio packet queue is serviced every 100 milliseconds.  Subsequent calls
     * to this method once running are ignored.
     */
    private void startProcessor()
    {
        if(mEnabled && mScheduledTask == null)
        {
            if(mThreadPoolManager != null)
            {
                mScheduledTask = mThreadPoolManager.scheduleFixedRate(ThreadPoolManager.ThreadType.AUDIO_PROCESSING,
                        mRecordingQueueProcessor, PROCESSOR_RUN_INTERVAL_MS, TimeUnit.MILLISECONDS );
            }
        }
    }

    /**
     * Stops the audio queue processor
     */
    private void stopProcessor()
    {
        if(mThreadPoolManager != null && mScheduledTask != null)
        {
            mThreadPoolManager.cancel(mScheduledTask);
        }
    }

    /**
     * Pauses or unpauses the broadcastAudio
     */
    public void setPaused(boolean paused)
    {
        if(!getBroadcastState().isErrorState())
        {
            if(paused)
            {
                setBroadcastState(BroadcastState.PAUSED);
            }
            else
            {
                setBroadcastState(BroadcastState.READY);
            }
        }
    }

    /**
     * Registers the listener to receive broadcastAudio state changes
     */
    public void setListener(Listener<BroadcastEvent> listener)
    {
        mBroadcastEventListener = listener;
    }

    /**
     * Removes the listener from receiving broadcastAudio state changes
     */
    public void removeListener()
    {
        mBroadcastEventListener = null;
    }

    /**
     * Broadcasts the event to any registered listener
     */
    public void broadcast(BroadcastEvent event)
    {
        if(mBroadcastEventListener != null)
        {
            mBroadcastEventListener.receive(event);
        }
    }

    /**
     * Sets the state of the broadcastAudio connection
     */
    protected void setBroadcastState(BroadcastState state)
    {
        if(mBroadcastState != state)
        {
            mLog.debug("Changing State to: " + state);
            mBroadcastState = state;

            broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_STATE_CHANGE));
        }
    }

    /**
     * Current state of the broadcastAudio connection
     */
    public BroadcastState getBroadcastState()
    {
        return mBroadcastState;
    }

    public boolean connected()
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
     * Audio recording queue processor.  Fetches recordings from the queue and chunks the recording byte content
     * to subclass implementations for broadcast in the appropriate manner.
     */
    public class RecordingQueueProcessor implements Runnable
    {
        private AtomicBoolean mProcessing = new AtomicBoolean();
        private ByteArrayInputStream mInputStream;
        private int mChunkSize;

        @Override
        public void run()
        {
            if(mProcessing.compareAndSet(false, true))
            {
                if(mInputStream == null || mInputStream.available() <= 0)
                {
                    nextRecording();
                }

                if(mInputStream != null)
                {
                    int length = Math.min(mChunkSize, mInputStream.available());

                    byte[] audio = new byte[length];

                    try
                    {
                        mInputStream.read(audio);

                        broadcastAudio(audio);
                    }
                    catch(IOException ioe)
                    {
                        mLog.error("Error reading from in-memory audio recording input stream", ioe);
                    }
                }
                else
                {
                    broadcastAudio(mSilenceFrame);
                }

                mProcessing.set(false);
            }
        }

        /**
         * Loads the next recording for broadcast
         */
        private void nextRecording()
        {
            if(mInputStream != null)
            {
                mStreamedAudioCount++;
                broadcast(new BroadcastEvent(AudioBroadcaster.this,
                        BroadcastEvent.Event.BROADCASTER_STREAMED_COUNT_CHANGE));
            }

            mInputStream = null;

            StreamableAudioRecording recording = mAudioRecordingQueue.poll();

            if(recording != null)
            {
                try
                {
                    byte[] audio = Files.readAllBytes(recording.getPath());

                    if(audio != null && audio.length > 0)
                    {
                        mInputStream = new ByteArrayInputStream(audio);

                        int wholeIntervalChunks = (int)(recording.getRecordingLength() / PROCESSOR_RUN_INTERVAL_MS);

                        //Check for divide by zero situation
                        if(wholeIntervalChunks == 0)
                        {
                            wholeIntervalChunks = 1;
                        }

                        mChunkSize = (int)(mInputStream.available() / wholeIntervalChunks) + 1;

                        broadcastMetadata(recording.getAudioMetadata());
                    }
                    else
                    {
                        broadcastMetadata(null);
                    }
                }
                catch(IOException ioe)
                {
                    mLog.error("Error reading temporary audio stream recording [" + recording.getPath().toString() +
                            "] - skipping recording");

                    mInputStream = null;
                }

                removeRecording(recording);

                broadcast(new BroadcastEvent(AudioBroadcaster.this, BroadcastEvent.Event.BROADCASTER_QUEUE_CHANGE));
            }
            else
            {
                broadcastMetadata(null);
            }
        }
    }
}
