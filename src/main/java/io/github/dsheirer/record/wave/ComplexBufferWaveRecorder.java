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

import io.github.dsheirer.dsp.filter.channelizer.ContinuousReusableBufferProcessor;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.ConversionUtils;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IReusableComplexBufferListener;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WAVE audio recorder module for recording complex (I&Q) samples to a wave file
 */
public class ComplexBufferWaveRecorder extends Module implements IReusableComplexBufferListener,
    Listener<ReusableComplexBuffer>, ISourceEventListener
{
    private final static Logger mLog = LoggerFactory.getLogger(ComplexBufferWaveRecorder.class);

    private ContinuousReusableBufferProcessor<ReusableComplexBuffer> mBufferProcessor =
        new ContinuousReusableBufferProcessor<>(500, 50);

    private AtomicBoolean mRunning = new AtomicBoolean();
    private ReusableBufferWaveWriter mWriter;
    private String mFilePrefix;
    private Path mFile;
    private AudioFormat mAudioFormat;

    public ComplexBufferWaveRecorder(float sampleRate, String filePrefix)
    {
        mFilePrefix = filePrefix;
        setSampleRate(sampleRate);
    }

    public void setSampleRate(float sampleRate)
    {
        if(mAudioFormat == null || mAudioFormat.getSampleRate() != sampleRate)
        {
            mAudioFormat = new AudioFormat(sampleRate, 16, 2, true, false);

            if(mRunning.get())
            {
                stop();
                start();
            }
        }
    }

    public Path getFile()
    {
        return mFile;
    }

    public void start()
    {
        if(mRunning.compareAndSet(false, true))
        {
            try
            {
                StringBuilder sb = new StringBuilder();
                sb.append(mFilePrefix);
                sb.append("_");
                sb.append(TimeStamp.getTimeStamp("_"));
                sb.append(".wav");
                mFile = Paths.get(sb.toString());

                mWriter = new ReusableBufferWaveWriter(mAudioFormat, mFile);

                mBufferProcessor.setListener(mWriter);
                mBufferProcessor.start();
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
            if(mBufferProcessor != null)
            {
                mBufferProcessor.stop();
                mBufferProcessor.setListener(null);
            }

            if(mWriter != null)
            {
                //Thread this operation so that it doesn't tie up the calling thread.  The wave writer
                //close method will also rename the file and this can sometimes take a few seconds.
                ThreadPool.SCHEDULED.schedule(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            mWriter.close();
                        }
                        catch(IOException ioe)
                        {
                            mLog.error("Error closing baseband I/Q recorder", ioe);
                        }
                    }
                }, 0, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void receive(ReusableComplexBuffer buffer)
    {
        //Queue the buffer with the buffer processor so that recording occurs on the buffer processor thread
        mBufferProcessor.receive(buffer);
    }

    @Override
    public Listener<ReusableComplexBuffer> getReusableComplexBufferListener()
    {
        return this;
    }

    @Override
    public void reset()
    {
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return new Listener<SourceEvent>()
        {
            @Override
            public void receive(SourceEvent sourceEvent)
            {
                switch(sourceEvent.getEvent())
                {
                    case NOTIFICATION_SAMPLE_RATE_CHANGE:
                        setSampleRate(sourceEvent.getValue().floatValue());
                        break;
                }
            }
        };
    }

    /**
     * Wave writer implementation for reusable complex buffers delivered from buffer processor
     */
    public class ReusableBufferWaveWriter extends WaveWriter implements Listener<List<ReusableComplexBuffer>>
    {
        public ReusableBufferWaveWriter(AudioFormat format, Path file) throws IOException
        {
            super(format, file);
        }

        @Override
        public void receive(List<ReusableComplexBuffer> reusableComplexBuffers)
        {
            boolean error = false;

            for(ReusableComplexBuffer reusableComplexBuffer: reusableComplexBuffers)
            {
                if(!error)
                {
                    try
                    {
                        mWriter.writeData(ConversionUtils.convertToSigned16BitSamples(reusableComplexBuffer));
                    }
                    catch(IOException ioe)
                    {
                        mLog.error("IOException while writing I/Q buffers to wave recorder - stopping recorder", ioe);
                        error = true;
                        stop();
                    }
                }

                reusableComplexBuffer.decrementUserCount();
            }
        }
    }
}
