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
import controller.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import record.AudioRecorder;
import sample.Listener;
import util.TimeStamp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class StreamManager implements Listener<AudioPacket>
{
    private final static Logger mLog = LoggerFactory.getLogger(StreamManager.class);
    private static final long MAXIMUM_RECORDER_LIFESPAN_MILLIS = 30000; //30 seconds

    private static AtomicInteger sNextRecordingNumber = new AtomicInteger();

    private ThreadPoolManager mThreadPoolManager;
    private Listener<AudioRecording> mAudioRecordingListener;
    private BroadcastFormat mBroadcastFormat;
    private Path mTempDirectory;
    private Map<Integer, AudioRecorder> mStreamRecorders = new HashMap<>();
    private Runnable mRecorderMonitor;
    private ScheduledFuture<?> mRecorderMonitorFuture;
    private AtomicBoolean mRunning = new AtomicBoolean();

    /**
     * Stream manager processes all incoming audio packets and reassembles individual audio streams, converts audio
     * to desired output format and persists each stream to disc.  Each recording is capped at a maximum length to
     * ensure that recordings don't run too long before they are streamed out and to ensure that inactive recordings
     * are closed in a timely fashion.
     *
     * Completed streamable audio recordings are nominated to the output listener (for broadcast) upon completion
     *
     * @param threadPoolManager for scheduling runnables
     * @param listener to receive completed audio recordings
     * @param tempDirectory where to store temporary audio recordings
     */
    public StreamManager(ThreadPoolManager threadPoolManager, Listener<AudioRecording> listener,
                         BroadcastFormat broadcastFormat, Path tempDirectory)
    {
        assert(tempDirectory != null && Files.isDirectory(tempDirectory));
        assert(mThreadPoolManager != null);

        mThreadPoolManager = threadPoolManager;
        mAudioRecordingListener = listener;
        mBroadcastFormat = broadcastFormat;
        mTempDirectory = tempDirectory;
    }

    /**
     * Starts the stream manager.  Schedules a processor to monitor for inactive temporary stream recorders
     * every 2 seconds.
     */
    public void start()
    {
        if(mRunning.compareAndSet(false, true))
        {
            if(mRecorderMonitor == null)
            {
                mRecorderMonitor = new RecorderMonitor();
            }

            mRecorderMonitorFuture = mThreadPoolManager.scheduleFixedRate(ThreadPoolManager.ThreadType.AUDIO_PROCESSING,
                    mRecorderMonitor, 2, TimeUnit.SECONDS);
        }
    }

    /**
     * Stops the stream manager.  Stops the inactive temporary stream recorder monitoring thread.
     */
    public void stop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            if(mRecorderMonitorFuture != null)
            {
                mRecorderMonitorFuture.cancel(true);
            }

            synchronized (mStreamRecorders)
            {
                List<Integer> streamKeys = new ArrayList<>(mStreamRecorders.keySet());

                for(Integer streamKey: streamKeys)
                {
                    removeRecorder(streamKey);
                }
            }
        }
    }

    @Override
    public void receive(AudioPacket audioPacket)
    {
        if(mRunning.get() && audioPacket.hasAudioMetadata())
        {
            synchronized (mStreamRecorders)
            {
                int sourceChannelID = audioPacket.getAudioMetadata().getSource();

                AudioPacket.Type type = audioPacket.getType();

                if(type == AudioPacket.Type.AUDIO)
                {
                    if(mStreamRecorders.containsKey(sourceChannelID))
                    {
                        AudioRecorder recorder = mStreamRecorders.get(sourceChannelID);

                        if(recorder != null)
                        {
                            recorder.receive(audioPacket);
                        }
                    }
                    else
                    {
                        AudioRecorder recorder = BroadcastFactory.getAudioRecorder(getTemporaryRecordingPath(),
                                mBroadcastFormat);
                        recorder.start(mThreadPoolManager.getScheduledExecutorService());
                        recorder.receive(audioPacket);
                        mStreamRecorders.put(sourceChannelID, recorder);
                    }
                }
                else if(type == AudioPacket.Type.END)
                {
                    removeRecorder(sourceChannelID);
                }
                else
                {
                    mLog.info("Unrecognized Audio Packet Type: " + type);
                }
            }
        }
    }

    /**
     * Removes the recorder associated with the source channel ID.
     *
     * Note: this method invocation is not thread safe and must be invoked by a thread safe mechanism that protects the
     * mStreamRecorders map.
     *
     * @param sourceChannelID identifying the recorder
     */
    private void removeRecorder(Integer sourceChannelID)
    {
        if(mStreamRecorders.containsKey(sourceChannelID))
        {
            AudioRecorder recorder = mStreamRecorders.remove(sourceChannelID);

            recorder.close(new Listener<AudioRecorder>()
            {
                @Override
                public void receive(AudioRecorder audioRecorder)
                {
                    AudioRecording audioRecording =
                        new AudioRecording(audioRecorder.getPath(), audioRecorder.getMetadata(),
                            audioRecorder.getTimeRecordingStart(), audioRecorder.getRecordingLength());

                    if(mAudioRecordingListener != null)
                    {
                        mAudioRecordingListener.receive(audioRecording);
                    }
                }
            });

        }
    }

    /**
     * Creates a temporary streaming recording file path
     */
    private Path getTemporaryRecordingPath()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(BroadcastModel.TEMPORARY_STREAM_FILE_SUFFIX);
        sb.append(sNextRecordingNumber.incrementAndGet()).append("_");
        sb.append( TimeStamp.getLongTimeStamp( "_" ) );
        sb.append( mBroadcastFormat.getFileExtension() );

        Path temporaryRecordingPath = mTempDirectory.resolve(sb.toString());

        return temporaryRecordingPath;
    }

    /**
     * Monitors recorders to ensure they don't exceed the maximum life-span allowed for a recording.
     */
    public class RecorderMonitor implements Runnable
    {
        @Override
        public void run()
        {
            synchronized (mStreamRecorders)
            {
                long now = System.currentTimeMillis();

                mStreamRecorders.entrySet().stream()
                    .filter(entry -> entry.getValue().getTimeRecordingStart() + MAXIMUM_RECORDER_LIFESPAN_MILLIS < now)
                    .forEach(entry ->
                {
                    mLog.debug("Maximum recording time limit reached - removing recorder: " +
                        entry.getValue().getPath().toString());

                    removeRecorder(entry.getKey());
                });
            }
        }
    }
}
