/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.source.wave;

import io.github.dsheirer.sample.ConversionUtils;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBufferQueue;
import io.github.dsheirer.source.ComplexSource;
import io.github.dsheirer.source.IControllableFileSource;
import io.github.dsheirer.source.IFrameLocationListener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ComplexWaveSource extends ComplexSource implements IControllableFileSource, AutoCloseable
{
    private final static Logger mLog = LoggerFactory.getLogger(ComplexWaveSource.class);

    private IFrameLocationListener mFrameLocationListener;
    private int mBytesPerFrame;
    private int mFrameCounter = 0;
    private long mFrequency = 0;
    private Listener<ReusableComplexBuffer> mListener;
    private AudioInputStream mInputStream;
    private File mFile;
    private ReusableComplexBufferQueue mReusableComplexBufferQueue = new ReusableComplexBufferQueue("ComplexWaveSource");
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
            throw new IOException("Empty or Unsupported file format");
        }

        mFile = file;
        mAutoReplay = autoReplay;
    }

    public ComplexWaveSource(File file) throws IOException
    {
        this(file, false);
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
        return (int)(getSampleRate() / 20.0d);
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
            long intervalMilliseconds = 50; //20 intervals per second
            double framesPerInterval = getSampleRate() / 20.0d;
            mReplayController = ThreadPool.SCHEDULED.scheduleAtFixedRate(new ReplayController(framesPerInterval),
                    0, intervalMilliseconds, TimeUnit.MILLISECONDS);


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
        // TODO Auto-generated method stub
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

                ReusableComplexBuffer reusableBuffer = mReusableComplexBufferQueue.getBuffer(samples.length);
                System.arraycopy(samples, 0, reusableBuffer.getSamples(), 0, samples.length);
                reusableBuffer.setTimestamp(System.currentTimeMillis());
                mListener.receive(reusableBuffer);
            }
        }
    }

    /**
     * Registers the listener to receive sample buffers as they are read from
     * the wave file
     */
    @Override
    public void setListener(Listener<ReusableComplexBuffer> listener)
    {
        mListener = listener;
    }

    /**
     * Unregisters the listener from receiving sample buffers
     */
    public void removeListener(Listener<ReusableComplexBuffer> listener)
    {
        mListener = null;
    }

    @Override
    public void dispose()
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
            int framesToRead = (int)Math.floor((mIntervals * mFramesPerInterval) - mFramesRead);

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
