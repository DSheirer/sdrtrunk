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
import io.github.dsheirer.audio.AudioPacket;
import io.github.dsheirer.channel.metadata.Metadata;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.ConversionUtils;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.TimeStamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WAVE audio recorder module for recording audio buffers to a wave file
 */
public class AudioPacketWaveRecorder extends Module implements Listener<AudioPacket>
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioPacketWaveRecorder.class);

    private WaveWriter mWriter;
    private String mFilePrefix;
    private Path mFile;
    private AudioFormat mAudioFormat;

    private BufferProcessor mBufferProcessor;
    private ScheduledFuture<?> mProcessorHandle;
    private LinkedBlockingQueue<AudioPacket> mAudioPackets = new LinkedBlockingQueue<>(500);
    private Metadata mMetadata;
    private long mLastBufferReceived;

    private AtomicBoolean mRunning = new AtomicBoolean();

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

    public void start(ScheduledExecutorService executor)
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
                sb.append(".wav");

                mFile = Paths.get(sb.toString());

                mWriter = new WaveWriter(mAudioFormat, mFile);

                if(mMetadata != null)
                {
                    mWriter.writeMetadata(WaveMetadata.createFrom(mMetadata));
                }

				/* Schedule the processor to run every 500 milliseconds */
                mProcessorHandle = executor.scheduleAtFixedRate(mBufferProcessor, 0, 500, TimeUnit.MILLISECONDS);
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
                write();

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

    @Override
    public void receive(AudioPacket audioPacket)
    {
        if(mRunning.get())
        {
            boolean success = mAudioPackets.offer(audioPacket);

            if(!success)
            {
                mLog.error("recorder buffer overflow - purging [" + mFile.toFile().getAbsolutePath() + "]");
                mAudioPackets.clear();
            }

            mLastBufferReceived = System.currentTimeMillis();
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
     * Writes all audio currently in the queue to the file
     * @throws IOException if there are any errors writing the audio
     */
    private void write() throws IOException
    {
        AudioPacket audioPacket = mAudioPackets.poll();

        while(audioPacket != null && audioPacket.getType() == AudioPacket.Type.AUDIO)
        {
            if(audioPacket.hasMetadata() && audioPacket.getMetadata().isUpdated())
            {
                if(mMetadata == null || mMetadata != audioPacket.getMetadata())
                {
                    mMetadata = audioPacket.getMetadata();
                    mWriter.writeMetadata(WaveMetadata.createFrom(mMetadata));
                }
            }

            mWriter.writeData(ConversionUtils.convertToSigned16BitSamples(audioPacket.getAudioBuffer()));
            audioPacket = mAudioPackets.poll();
        }
    }

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
                mAudioPackets.clear();
                stop();

                mLog.error("IO Exception while trying to write to the wave writer", ioe);
            }
        }
    }
}
