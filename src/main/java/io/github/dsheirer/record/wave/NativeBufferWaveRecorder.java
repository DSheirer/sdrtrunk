/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.record.wave;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.ConversionUtils;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.util.Dispatcher;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;

/**
 * WAVE audio recorder module for recording complex (I&Q) samples to a wave file
 */
public class NativeBufferWaveRecorder extends Module implements Listener<INativeBuffer>, ISourceEventListener
{
    private static final Logger mLog = LoggerFactory.getLogger(ComplexSamplesWaveRecorder.class);
    private static final long STATUS_UPDATE_BYTE_INTERVAL = 1_048_576;
    private static final long MAX_RECORDING_SIZE = (long)Integer.MAX_VALUE * 2l;
    private Dispatcher<INativeBuffer> mBufferProcessor = new Dispatcher<>("sdrtrunk native buffer wave recorder", 100, 250);

    private AtomicBoolean mRunning = new AtomicBoolean();
    private NativeBufferWaveWriter mWriter;
    private String mFilePrefix;
    private AudioFormat mAudioFormat;
    private IRecordingStatusListener mStatusListener;
    private String mFilePath;
    private long mCurrentSize = 0;
    private long mLastReportedSize = 0;
    private int mRecordingCount = 0;

    public NativeBufferWaveRecorder(float sampleRate, String filePrefix, IRecordingStatusListener statusListener)
    {
        mFilePrefix = filePrefix;
        mStatusListener = statusListener;
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

    private String getFileName()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(mFilePrefix);
        sb.append("_");
        sb.append(TimeStamp.getTimeStamp("_"));
        sb.append(".wav");
        return sb.toString();
    }

    public void start()
    {
        if(mRunning.compareAndSet(false, true))
        {
            mRecordingCount = 1;
            mCurrentSize = 0;
            mLastReportedSize = 0;

            try
            {
                mFilePath = getFileName();
                mWriter = new NativeBufferWaveWriter(mAudioFormat, Paths.get(mFilePath));
                mBufferProcessor.setListener(mWriter);
                mStatusListener.update(mRecordingCount, mFilePath, 0);
                mBufferProcessor.start();
            }
            catch(IOException io)
            {
                mLog.error("Error starting complex baseband recorder", io);
            }
        }
    }

    /**
     * Rollover the recording once the current recording file size is full
     */
    private void rollRecording()
    {
        if(mWriter != null)
        {
            try
            {
                mWriter.close();
                mWriter = null;
            }
            catch(IOException ioe)
            {
                mLog.error("Error closing recording during file rollover");
            }

            mCurrentSize = 0;
            mLastReportedSize = 0;

            try
            {
                mFilePath = getFileName();
                mWriter = new NativeBufferWaveWriter(mAudioFormat, Paths.get(mFilePath));
                mBufferProcessor.setListener(mWriter);
                mStatusListener.update(++mRecordingCount, mFilePath, 0);
            }
            catch(IOException ioe)
            {
                mLog.error("Error creating new recording during file rollover");
                stop();
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
                ThreadPool.CACHED.submit(() ->
                {
                    try
                    {
                        mWriter.close();
                    }
                    catch(IOException ioe)
                    {
                        mLog.error("Error closing baseband I/Q recorder", ioe);
                    }
                });
            }
        }
    }

    @Override
    public void receive(INativeBuffer nativeBuffer)
    {
        if(mRunning.get())
        {
            //Queue the buffer with the buffer processor so that recording occurs on the buffer processor thread
            mBufferProcessor.receive(nativeBuffer);
        }
    }

    public Listener<INativeBuffer> getReusableComplexBufferListener()
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
        return sourceEvent ->
        {
            switch(sourceEvent.getEvent())
            {
                case NOTIFICATION_SAMPLE_RATE_CHANGE:
                    setSampleRate(sourceEvent.getValue().floatValue());
                    break;
            }
        };
    }

    /**
     * Wave writer implementation for complex samples delivered from buffer processor
     */
    public class NativeBufferWaveWriter extends WaveWriter implements Listener<INativeBuffer>
    {
        public NativeBufferWaveWriter(AudioFormat format, Path file) throws IOException
        {
            super(format, file);
        }

        @Override
        public void receive(INativeBuffer nativeBuffer)
        {
            boolean error = false;

            Iterator<InterleavedComplexSamples> iterator = nativeBuffer.iteratorInterleaved();

            while(iterator.hasNext() & !error)
            {
                try
                {
                    ByteBuffer data = ConversionUtils.convertToSigned16BitSamples(iterator.next());

                    if((mCurrentSize + data.array().length) > MAX_RECORDING_SIZE)
                    {
                        rollRecording();
                    }

                    mWriter.writeData(data);

                    mCurrentSize += data.array().length;
                    if(mCurrentSize > (mLastReportedSize + STATUS_UPDATE_BYTE_INTERVAL))
                    {
                        mStatusListener.update(mRecordingCount, mFilePath, mCurrentSize);
                        mLastReportedSize = mCurrentSize;
                    }
                }
                catch(IOException ioe)
                {
                    mLog.error("I/O exception while writing I/Q buffers to wave recorder - stopping recorder", ioe);
                    error = true;
                    stop();
                }
            }
        }
    }
}
