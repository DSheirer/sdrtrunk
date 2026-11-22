/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

import io.github.dsheirer.module.Module;
import io.github.dsheirer.record.BasebandRecordFormat;
import io.github.dsheirer.sample.ConversionUtils;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.IComplexSamplesListener;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.util.Dispatcher;
import io.github.dsheirer.util.ThreadPool;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;

/**
 * Baseband recorder module for recording complex (I&Q) samples to a wave (.wav) or SDRSharp raw I/Q (.iq) file.
 *
 * The recording file is created when the first sample buffer arrives so that the file name reflects the current
 * sample rate and no empty file is left behind when a sample rate change restarts the recorder.
 */
public class ComplexSamplesWaveRecorder extends Module implements IComplexSamplesListener,
        Listener<ComplexSamples>, ISourceEventListener
{
    private final static Logger mLog = LoggerFactory.getLogger(ComplexSamplesWaveRecorder.class);

    private Dispatcher<ComplexSamples> mBufferProcessor = new Dispatcher<>("sdrtrunk complex baseband recorder", 250);
    private AtomicBoolean mRunning = new AtomicBoolean();
    private final Object mWriterLock = new Object();
    private final BasebandRecordFormat mRecordFormat;
    private final IRecordingPathProvider mPathProvider;
    private IBasebandRecordingWriter mWriter;
    private Path mFile;
    private AudioFormat mAudioFormat;

    /**
     * Constructs a wave format recorder.
     * @param sampleRate initial sample rate
     * @param filePrefix full path and file name, without the .wav extension
     */
    public ComplexSamplesWaveRecorder(float sampleRate, String filePrefix)
    {
        this(sampleRate, BasebandRecordFormat.WAVE, rate -> Paths.get(filePrefix + BasebandRecordFormat.WAVE.getExtension()));
    }

    /**
     * Constructs an instance
     * @param sampleRate initial sample rate
     * @param recordFormat for the recording file
     * @param pathProvider to provide the recording file path each time a new recording file is created.
     */
    public ComplexSamplesWaveRecorder(float sampleRate, BasebandRecordFormat recordFormat,
                                      IRecordingPathProvider pathProvider)
    {
        mRecordFormat = recordFormat;
        mPathProvider = pathProvider;
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

    /**
     * Current recording file or null if a recording file has not yet been created.
     */
    public Path getFile()
    {
        return mFile;
    }

    public void start()
    {
        if(mRunning.compareAndSet(false, true))
        {
            mBufferProcessor.setListener(this::write);
            mBufferProcessor.start();
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

            closeWriter();
        }
    }

    public void flushAndStop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            if(mBufferProcessor != null)
            {
                mBufferProcessor.flushAndStop();
                mBufferProcessor.setListener(null);
            }

            closeWriter();
        }
    }

    /**
     * Closes the current writer on a separate thread so that it doesn't tie up the calling thread.
     */
    private void closeWriter()
    {
        IBasebandRecordingWriter writer;

        synchronized(mWriterLock)
        {
            writer = mWriter;
            mWriter = null;
        }

        if(writer != null)
        {
            ThreadPool.CACHED.submit(() ->
            {
                try
                {
                    writer.close();
                }
                catch(IOException ioe)
                {
                    mLog.error("Error closing baseband I/Q recorder", ioe);
                }
            });
        }
    }

    /**
     * Writes the samples to the recording, creating the recording file on first use.  Invoked on the dispatcher
     * thread.
     */
    private void write(ComplexSamples complexSamples)
    {
        boolean error = false;

        synchronized(mWriterLock)
        {
            if(!mRunning.get())
            {
                return;
            }

            try
            {
                if(mWriter == null)
                {
                    mFile = mPathProvider.getPath(mAudioFormat.getSampleRate());
                    mWriter = IBasebandRecordingWriter.create(mRecordFormat, mAudioFormat, mFile);
                }

                mWriter.writeData(ConversionUtils.convertToSigned16BitSamples(complexSamples));
            }
            catch(IOException ioe)
            {
                mLog.error("IOException while writing I/Q buffers to baseband recorder [" + mFile +
                    "] - stopping recorder", ioe);
                error = true;
            }
        }

        if(error)
        {
            stop();
        }
    }

    @Override
    public void receive(ComplexSamples buffer)
    {
        //Queue the buffer with the buffer processor so that recording occurs on the buffer processor thread
        mBufferProcessor.receive(buffer);
    }

    @Override
    public Listener<ComplexSamples> getComplexSamplesListener()
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
}
