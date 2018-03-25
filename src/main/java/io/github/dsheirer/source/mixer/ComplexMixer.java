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
package io.github.dsheirer.source.mixer;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.adapter.IComplexSampleAdapter;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBufferQueue;
import io.github.dsheirer.source.SourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.util.concurrent.atomic.AtomicBoolean;

public class ComplexMixer
{
    private final static Logger mLog = LoggerFactory.getLogger(ComplexMixer.class);

    private long mFrequency = 0;

    private BufferReader mBufferReader = new BufferReader();
    private TargetDataLine mTargetDataLine;
    private AudioFormat mAudioFormat;
    private String mName;
    private int mBytesPerFrame = 0;
    private IComplexSampleAdapter mSampleAdapter;
    private Listener<ReusableComplexBuffer> mListener;
    private ReusableComplexBufferQueue mReusableComplexBufferQueue = new ReusableComplexBufferQueue("ComplexMixer");

    /**
     * Complex Mixer - constructs a reader on the mixer/sound card target data line using the specified audio
     * format (sample size, sample rate ) and broadcasts complex (I/Q) sample buffers to all registered listeners.
     * Reads sample buffers sized to 10% of the sample rate specified in audio format.
     *
     * @param targetDataLine - mixer or sound card to be used
     * @param format - audio format
     * @param name - token name to use for this mixer
     * @param complexSampleAdapter - adapter to convert byte array data read from the mixer into ComplexBuffer.
     * @param listener to receive generated complex buffers
     */
    public ComplexMixer(TargetDataLine targetDataLine, AudioFormat format, String name,
                        IComplexSampleAdapter complexSampleAdapter, Listener<ReusableComplexBuffer> listener)
    {
        mTargetDataLine = targetDataLine;
        mName = name;
        mAudioFormat = format;
        mSampleAdapter = complexSampleAdapter;
        mListener = listener;
    }

    public void start()
    {
        if(!mBufferReader.isRunning())
        {
            Thread thread = new Thread(mBufferReader);
            thread.setDaemon(true);
            thread.setName(mName + " Complex Sample Reader");
            thread.start();
        }
    }

    public void stop()
    {
        mBufferReader.stop();
    }

    @Override
    public String toString()
    {
        return mName;
    }

    public int getSampleRate()
    {
        if(mTargetDataLine != null)
        {
            return (int)mTargetDataLine.getFormat().getSampleRate();
        }
        else
        {
            return 0;
        }
    }

    /**
     * Returns the frequency of this source.  Default is 0.
     */
    public long getFrequency() throws SourceException
    {
        return mFrequency;
    }

    /**
     * Specify the frequency that will be returned from this source.  This may
     * be useful if you are streaming an external audio source in through the
     * sound card and you want to specify a frequency for that source
     */
    public void setFrequency(long frequency)
    {
        mFrequency = frequency;
    }

    /**
     * Reader thread.  Performs blocking read against the mixer target data
     * line, converts the samples to an array of floats using the specified
     * adapter.  Dispatches float arrays to all registered listeners.
     */
    public class BufferReader implements Runnable
    {
        private AtomicBoolean mRunning = new AtomicBoolean();

        @Override
        public void run()
        {
            if(mRunning.compareAndSet(false, true))
            {
                if(mTargetDataLine == null)
                {
                    mRunning.set(false);
                    mLog.error("ComplexMixerSource - mixer target data line is null");
                }
                else
                {
                    mBytesPerFrame = mAudioFormat.getSampleSizeInBits() / 8;

                    if(mBytesPerFrame == AudioSystem.NOT_SPECIFIED)
                    {
                        mBytesPerFrame = 2;
                    }

                    //Set buffer size to 1/10 second of samples
                    int sampleCount = (int)(mAudioFormat.getSampleRate() * 0.10) * 2;
                    int bufferSize = sampleCount * mBytesPerFrame;

                    //We'll reuse the same buffer for each read
                    byte[] buffer = new byte[bufferSize];

                    try
                    {
                        mTargetDataLine.open(mAudioFormat);
                        mTargetDataLine.start();
                    }
                    catch(LineUnavailableException e)
                    {
                        mLog.error("ComplexMixerSource - mixer target data line not available to read data from", e);
                        mRunning.set(false);
                    }

                    while(mRunning.get())
                    {
                        try
                        {
                            //Blocking read - waits until the buffer fills
                            mTargetDataLine.read(buffer, 0, buffer.length);

                            /* Dispatch samples to registered listeners */
                            if(mListener != null)
                            {
                                ReusableComplexBuffer reusableComplexBuffer = mReusableComplexBufferQueue.getBuffer(sampleCount);

                                //Convert samples and load into reusable buffer
                                mSampleAdapter.convertAndLoad(buffer, reusableComplexBuffer);

                                mListener.receive(reusableComplexBuffer);
                            }
                        }
                        catch(Exception e)
                        {
                            mLog.error("error while reading from the mixer target data line", e);
                            mRunning.set(false);
                        }
                    }
                }

                /* Close the data line if it is still open */
                if(mTargetDataLine != null && mTargetDataLine.isOpen())
                {
                    mTargetDataLine.close();
                }
            }
        }

        /**
         * Stops the reader thread
         */
        public void stop()
        {
            if(mRunning.compareAndSet(true, false))
            {
                mTargetDataLine.stop();
                mTargetDataLine.close();
            }
        }

        /**
         * Indicates if the reader thread is running
         */
        public boolean isRunning()
        {
            return mRunning.get();
        }
    }

    public void dispose()
    {
        if(mBufferReader != null)
        {
            mBufferReader.stop();
        }

        mListener = null;
    }
}
