/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.record.wave;

import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.ConversionUtils;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IReusableBufferDisposedListener;
import io.github.dsheirer.sample.buffer.IReusableComplexBufferListener;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WAVE audio recorder module for recording complex (I&Q) samples to a wave file
 */
public class ComplexBufferWaveRecorder extends Module implements IReusableComplexBufferListener, Listener<ReusableComplexBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger(ComplexBufferWaveRecorder.class);

    private WaveWriter mWriter;
    private String mFilePrefix;
    private Path mFile;
    private AudioFormat mAudioFormat;

    private BufferProcessor mBufferProcessor;
    private ScheduledFuture<?> mProcessorHandle;
    private LinkedBlockingQueue<ReusableComplexBuffer> mBuffers = new LinkedBlockingQueue<>(500);

    private AtomicBoolean mRunning = new AtomicBoolean();

    public ComplexBufferWaveRecorder(int sampleRate, String filePrefix)
    {
        mAudioFormat = new AudioFormat(sampleRate, 16, 2, true, false);
        mFilePrefix = filePrefix;
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
                sb.append(TimeStamp.getTimeStamp("_"));
                sb.append(".wav");

                mFile = Paths.get(sb.toString());

                mWriter = new WaveWriter(mAudioFormat, mFile);

                /* Schedule the processor to run every 500 milliseconds */
                mProcessorHandle = ThreadPool.SCHEDULED.scheduleAtFixedRate(
                    mBufferProcessor, 0, 500, TimeUnit.MILLISECONDS);
            }
            catch(IOException io)
            {
                mLog.error("Error starting complex baseband recorder", io);
            }
        }
    }

    public void stop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            receive(new PoisonPill());
        }
    }

    @Override
    public void receive(ReusableComplexBuffer buffer)
    {
        if(mRunning.get())
        {
            boolean success = mBuffers.offer(buffer);

            if(!success)
            {
                mLog.error("recorder buffer overflow - purging [" +
                    mFile.toFile().getAbsolutePath() + "]");

                mBuffers.clear();
            }
        }
    }

    @Override
    public Listener<ReusableComplexBuffer> getReusableComplexBufferListener()
    {
        return this;
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

    public class BufferProcessor implements Runnable
    {
        public void run()
        {
            try
            {
                ReusableComplexBuffer reusableComplexBuffer = mBuffers.poll();

                while(reusableComplexBuffer != null)
                {
                    if(reusableComplexBuffer instanceof PoisonPill)
                    {
                        //Clear the buffer queue
                        reusableComplexBuffer = mBuffers.poll();

                        while(reusableComplexBuffer != null)
                        {
                            reusableComplexBuffer.decrementUserCount();
                            reusableComplexBuffer = mBuffers.poll();
                        }

                        if(mWriter != null)
                        {
                            try
                            {
                                mWriter.close();
                                mWriter = null;
                            }
                            catch(IOException io)
                            {
                                mLog.error("Error stopping complex wave recorder [" + getFile() + "]", io);
                            }
                        }

                        mFile = null;

                        if(mProcessorHandle != null)
                        {
                            mProcessorHandle.cancel(true);
                        }

                        mProcessorHandle = null;
                    }
                    else
                    {
                        mWriter.writeData(ConversionUtils.convertToSigned16BitSamples(reusableComplexBuffer));
                        reusableComplexBuffer.decrementUserCount();
                        reusableComplexBuffer = mBuffers.poll();
                    }
                }
            }
            catch(IOException ioe)
            {
                /* Stop this module if/when we get an IO exception */
                mBuffers.clear();
                stop();

                mLog.error("IOException while trying to write to the wave writer", ioe);
            }
        }
    }

    /**
     * This is used as a sentinel value to signal the buffer processor to end
     */
    public class PoisonPill extends ReusableComplexBuffer
    {
        public PoisonPill()
        {
            super(new IReusableBufferDisposedListener<ReusableComplexBuffer>()
            {
                @Override
                public void disposed(ReusableComplexBuffer reusableComplexBuffer)
                {
                    //no-op
                }
            }, new float[2]);
        }
    }
}
