/*
 * *********************************************************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2017 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 * *********************************************************************************************************************
 */
package io.github.dsheirer.record.wave;

import io.github.dsheirer.audio.AudioFormats;
import io.github.dsheirer.channel.metadata.Metadata;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.ConversionUtils;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.OverflowableReusableBufferTransferQueue;
import io.github.dsheirer.sample.buffer.ReusableAudioPacket;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WAVE audio recorder module for recording audio buffers to a wave file
 */
public class AudioPacketWaveRecorder extends Module implements Listener<ReusableAudioPacket>
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioPacketWaveRecorder.class);

    private WaveWriter mWriter;
    private String mFilePrefix;
    private Path mFile;
    private AudioFormat mAudioFormat;

    private OverflowableReusableBufferTransferQueue<ReusableAudioPacket> mTransferQueue =
        new OverflowableReusableBufferTransferQueue<>(500, 100);
    private BufferProcessor mBufferProcessor;
    private ScheduledFuture<?> mProcessorHandle;
    private Metadata mMetadata;
    private long mLastBufferReceived;

    private AtomicBoolean mRunning = new AtomicBoolean();

    /**
     * Wave audio recorder for AudioPackets
     * @param filePrefix
     * @param metadata
     */
    public AudioPacketWaveRecorder(String filePrefix, Metadata metadata)
    {
        mMetadata = metadata;
        mFilePrefix = filePrefix;
        mAudioFormat = AudioFormats.PCM_SIGNED_8KHZ_16BITS_MONO;
    }

    /**
     * Indicates if the recorder is currently running.
     */
    public boolean isRunning()
    {
        return mRunning.get();
    }

    /**
     * Timestamp of when the latest buffer was received by this recorder
     */
    public long getLastBufferReceived()
    {
        return mLastBufferReceived;
    }

    public Path getFile()
    {
        return mFile;
    }

    public void start()
    {
        if(mRunning.compareAndSet(false, true))
        {
            if(mBufferProcessor == null)
            {
                mBufferProcessor = new BufferProcessor();
            }

            try
            {
                StringBuilder sb = new StringBuilder();
                sb.append(mFilePrefix);
                sb.append("_");
                sb.append(TimeStamp.getLongTimeStamp("_"));
                sb.append(".tmp");

                mFile = Paths.get(sb.toString());

                mWriter = new WaveWriter(mAudioFormat, mFile);

				/* Schedule the processor to run every 500 milliseconds */
                mProcessorHandle = ThreadPool.SCHEDULED.scheduleAtFixedRate(mBufferProcessor, 0, 500, TimeUnit.MILLISECONDS);
            }
            catch(IOException io)
            {
                mLog.error("Error starting real buffer recorder", io);
            }
        }
    }

    public void stop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            try
            {
                //Finish writing any residual audio buffers
                write();

                //Append the LIST and ID3 metadata to the end
                if(mMetadata != null)
                {
                    mWriter.writeMetadata(WaveMetadata.createFrom(mMetadata));
                    mMetadata = null;
                }

                if(mWriter != null)
                {
                    mWriter.close();
                    mWriter = null;
                }
            }
            catch(IOException ioe)
            {
                mLog.error("Error writing final audio buffers to recording during shutdown", ioe);
            }
        }
    }

    /**
     * Primary input method for receiving audio packets to enqueue for later writing to the wav file.
     */
    @Override
    public void receive(ReusableAudioPacket audioPacket)
    {
        if(mRunning.get())
        {
            mTransferQueue.offer(audioPacket);
            mLastBufferReceived = System.currentTimeMillis();
        }
        else
        {
            audioPacket.decrementUserCount();
        }
    }

    @Override
    public void dispose()
    {
        stop();
    }

    @Override
    public void reset()
    {
    }

    /**
     * Writes all audio currently in the queue to the file.  Captures any audio metadata from the packet and retains a
     * copy of the latest metadata to append to the end of the recording when stop() is invoked.
     *
     * @throws IOException if there are any errors writing the audio
     */
    private void write() throws IOException
    {
        ReusableAudioPacket audioPacket = mTransferQueue.poll();

        while(audioPacket != null && audioPacket.getType() == ReusableAudioPacket.Type.AUDIO)
        {
            if(audioPacket.hasMetadata())
            {
                mMetadata = audioPacket.getMetadata();
            }

            mWriter.writeData(ConversionUtils.convertToSigned16BitSamples(audioPacket.getAudioSamples()));
            audioPacket.decrementUserCount();
            audioPacket = mTransferQueue.poll();
        }

        if(audioPacket != null)
        {
            audioPacket.decrementUserCount();
        }
    }

    /**
     * Scheduled runnable to periodically write the enqueued audio packets to the file
     */
    public class BufferProcessor implements Runnable
    {
        public void run()
        {
            try
            {
                write();
            }
            catch(IOException ioe)
            {
				/* Stop this module if/when we get an IO exception */
                mTransferQueue.clear();
                stop();

                mLog.error("IO Exception while trying to write to the wave writer", ioe);
            }
        }
    }
}
