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
package io.github.dsheirer.source.wave;

import io.github.dsheirer.buffer.FloatNativeBuffer;
import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.sample.ConversionUtils;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.SampleType;
import io.github.dsheirer.source.IControllableFileSource;
import io.github.dsheirer.source.IFrameLocationListener;
import io.github.dsheirer.source.Source;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.util.ThreadPool;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class ComplexWaveSource extends Source implements IControllableFileSource, AutoCloseable
{
    private final static Logger mLog = LoggerFactory.getLogger(ComplexWaveSource.class);

    private IFrameLocationListener mFrameLocationListener;
    private int mBufferSampleCount = 65536; //Complex samples per buffer
    private int mBytesPerFrame;
    private int mFrameCounter = 0;
    private long mFrequency = 0;
    private Listener<INativeBuffer> mListener;
    private AudioInputStream mInputStream;
    private File mFile;
    private boolean mAutoReplay;
    private ScheduledFuture<?> mReplayController;

    /**
     * Constructs an instance with optional auto-replay at near real time.
     * @param file containing complex I/Q sample data
     * @param autoReplay to enable continuous looping, real-time playback of sample data
     */
    public ComplexWaveSource(File file, boolean autoReplay) throws IOException
    {
        if(file == null || !file.exists() || !supports(file))
        {
            throw new IOException("Empty or null file");
        }

        if(!supports(file))
        {
            throw new IOException("Unsupported file format");
        }

        mFile = file;
        mAutoReplay = autoReplay;
    }

    public ComplexWaveSource(File file) throws IOException
    {
        this(file, false);
    }

    @Override public SampleType getSampleType()
    {
        return SampleType.COMPLEX;
    }

    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        //Not implemented
    }

    @Override
    public void removeSourceEventListener()
    {
        //Not implemented
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        //Not implemented
        return null;
    }

    @Override
    public void reset()
    {
        stop();
        mFrameCounter = 0;
        start();
    }

    /**
     * Number of samples per buffer
     */
    public int getBufferSampleCount()
    {
        return mBufferSampleCount;
//        return (int)(getSampleRate() / 20.0d);
    }

    /**
     * Audio format for the currently opened or started source file.
     */
    public AudioFormat getAudioFormat()
    {
        if(mInputStream == null)
        {
            throw new IllegalStateException("Source not opened or started");
        }

        return mInputStream.getFormat();
    }

    @Override
    public void start()
    {
        if(mInputStream == null)
        {
            try
            {
                open();
            }
            catch(Exception e)
            {
                mLog.error("Error", e);
            }
        }

        if(mAutoReplay)
        {
            double sampleRate = getSampleRate();

            double buffersPerSecond = (sampleRate / mBufferSampleCount);
            long intervalMilliseconds = (long)(1000.0 / buffersPerSecond);
            Runnable r = new ReplayController(mBufferSampleCount);
            mReplayController = ThreadPool.SCHEDULED.scheduleAtFixedRate(r, 0, intervalMilliseconds, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void stop()
    {
        try
        {
            if(mReplayController != null)
            {
                mReplayController.cancel(true);
            }

            close();
        }
        catch(IOException e)
        {
            mLog.error("Error stopping complex wave source");
        }
    }

    @Override
    public long getFrameCount() throws IOException
    {
        return 0;
    }

    @Override
    public double getSampleRate()
    {
        if(mInputStream != null)
        {
            return mInputStream.getFormat().getSampleRate();
        }

        return 0;
    }

    /**
     * Returns the frequency set for this file.  Normally returns zero, but
     * the value can be set with setFrequency() method.
     */
    public long getFrequency()
    {
        return mFrequency;
    }

    /**
     * Changes the value returned from getFrequency() for this source.
     */
    public void setFrequency(long frequency)
    {
        mFrequency = frequency;
    }

    /**
     * Closes the source file
     */
    public void close() throws IOException
    {
        if(mInputStream != null)
        {
            mInputStream.close();
            mInputStream = null;
        }
    }

    /**
     * Opens the source file for reading
     */
    public void open() throws IOException, UnsupportedAudioFileException
    {
        if(mInputStream == null)
        {
            mInputStream = AudioSystem.getAudioInputStream(mFile);

            AudioFormat format = mInputStream.getFormat();

            mBytesPerFrame = format.getFrameSize();

            if(format.getChannels() != 2 || format.getSampleSizeInBits() != 16)
            {
                throw new IOException("Unsupported Wave Format - EXPECTED: 2 " +
                        "channels 16-bit samples FOUND: " +
                        mInputStream.getFormat().getChannels() + " channels " +
                        mInputStream.getFormat().getSampleSizeInBits() + "-bit samples");
            }

            /* Broadcast that we're at frame location 0 */
            broadcast(0);
        }
    }

    /**
     * Reads the number of frames and sends a buffer to the listener
     */
    @Override
    public void next(int frames) throws IOException
    {
        next(frames, true);
    }

    /**
     * Reads the number of frames and optionally sends the buffer(s) to the listener
     */
    public void next(int frames, boolean broadcast) throws IOException
    {
        if(mInputStream != null)
        {
            byte[] buffer = new byte[mBytesPerFrame * frames];

        	/* Fill the buffer with samples from the file */
            int samplesRead = mInputStream.read(buffer);

            mFrameCounter += samplesRead;

            broadcast(mFrameCounter);

            if(broadcast && mListener != null)
            {
                if(samplesRead < 0)
                {
                    throw new IOException("End of file reached");
                }

                if(samplesRead < buffer.length)
                {
                    buffer = Arrays.copyOf(buffer, samplesRead);
                }

                float[] samples = ConversionUtils.convertFromSigned16BitSamples(buffer);
                mListener.receive(new FloatNativeBuffer(samples, System.currentTimeMillis(),
                        mInputStream.getFormat().getSampleRate() / 1000.0f));
            }
        }
    }

    /**
     * Registers the listener to receive sample buffers as they are read from
     * the wave file
     */
    public void setListener(Listener<INativeBuffer> listener)
    {
        mListener = listener;
    }

    /**
     * Unregisters the listener from receiving sample buffers
     */
    public void removeListener(Listener<INativeBuffer> listener)
    {
        mListener = null;
    }

    @Override
    public File getFile()
    {
        return mFile;
    }

    private void broadcast(int byteLocation)
    {
        int frameLocation = (int)(byteLocation / mBytesPerFrame);

        if(mFrameLocationListener != null)
        {
            mFrameLocationListener.frameLocationUpdated(frameLocation);
        }
    }

    @Override
    public void setListener(IFrameLocationListener listener)
    {
        mFrameLocationListener = listener;
    }

    @Override
    public void removeListener(IFrameLocationListener listener)
    {
        mFrameLocationListener = null;
    }

    /**
     * Indicates if the file is a supported audio file type
     */
    public static boolean supports(File file)
    {
        try(AudioInputStream ais = AudioSystem.getAudioInputStream(file))
        {
            AudioFormat format = ais.getFormat();

            if(format.getChannels() == 2 && format.getSampleSizeInBits() == 16)
            {
                return true;
            }
        }
        catch(Exception e)
        {
            //Do nothing, we'll return a default of false
        }

        return false;
    }

    public class ReplayController implements Runnable
    {
        private double mFramesPerInterval;
        private int mFramesRead;
        private int mIntervals;

        public ReplayController(double framesPerInterval)
        {
            mFramesPerInterval = framesPerInterval;
        }

        @Override
        public void run()
        {
            mIntervals++;
            int framesToRead = (int) FastMath.floor((mIntervals * mFramesPerInterval) - mFramesRead);

            try
            {
                next(framesToRead, true);
                mFramesRead += framesToRead;
            }
            catch(IOException ioe)
            {
                mLog.debug("End of Recording - looping [" + ioe.getLocalizedMessage() + "]");
                reset();
            }
        }
    }
}
