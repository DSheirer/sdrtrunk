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
import io.github.dsheirer.sample.adapter.AbstractSampleAdapter;
import io.github.dsheirer.sample.buffer.ReusableFloatBuffer;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.heartbeat.HeartbeatManager;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Target Data Line sample data processor.  Performs blocking read against the mixer target data line, converts
 * the samples to an array of floats using the specified adapter.  Dispatches float arrays to the registered
 * buffer listener.
 */
public class MixerReader<T extends ReusableFloatBuffer> implements Runnable
{
    private final static Logger mLog = LoggerFactory.getLogger(MixerReader.class);

    private static final long BUFFER_PROCESSING_INTERVAL_MS = 50; //50 milliseconds, 20 reads per second
    private TargetDataLine mTargetDataLine;
    private byte[] mBuffer;
    private int mBufferSize;
    private AtomicBoolean mRunning = new AtomicBoolean();
    private ScheduledFuture mScheduledFuture;
    private int mBytesRead;
    private AbstractSampleAdapter<T> mSampleAdapter;
    private Listener<T> mReusableBufferListener;
    private Listener<SourceEvent> mSourceEventListener;
    private AudioFormat mAudioFormat;
    private HeartbeatManager mHeartbeatManager;

    public MixerReader(AudioFormat audioFormat, TargetDataLine targetDataLine,
                       AbstractSampleAdapter<T> abstractSampleAdapter, HeartbeatManager heartbeatManager)
    {
        mTargetDataLine = targetDataLine;
        mAudioFormat = audioFormat;
        mSampleAdapter = abstractSampleAdapter;
        mHeartbeatManager = heartbeatManager;

        /* Set buffer size to 1/10 second of samples */
        mBufferSize = (int)(mAudioFormat.getSampleRate() * 0.1) * mAudioFormat.getFrameSize();
    }

    public MixerReader(AudioFormat audioFormat, TargetDataLine targetDataLine,
                       AbstractSampleAdapter<T> abstractSampleAdapter)
    {
        this(audioFormat, targetDataLine, abstractSampleAdapter, null);
    }

    /**
     * Audio format used by this reader
     */
    public AudioFormat getAudioFormat()
    {
        return mAudioFormat;
    }

    /**
     * Sets the size of buffers to use.  This reader will read from target data line 20 times per second.
     * @param bytesPerBuffer to size for each buffer read.
     */
    public void setBufferSize(int bytesPerBuffer)
    {
        mBufferSize = bytesPerBuffer;
        mBuffer = new byte[mBufferSize];
    }

    /**
     * Opens the source mixer target data line
     *
     * @throws LineUnavailableException if the target data line is null or unavailable (from the OS).
     */
    private void openTargetDataLine() throws LineUnavailableException
    {
        if(mTargetDataLine == null)
        {
            throw new LineUnavailableException("Source Mixer TargetDataLine is null");
        }

        mBuffer = new byte[mBufferSize];

        mTargetDataLine.open(mAudioFormat);
        mTargetDataLine.start();
    }

    /**
     * Closes the mixer source target data line
     */
    private void closeTargetDataLine()
    {
        if(mTargetDataLine != null && mTargetDataLine.isOpen())
        {
            mTargetDataLine.close();
        }
    }

    /**
     * Starts the sample processing reader thread
     */
    public void start()
    {
        if(mRunning.compareAndSet(false, true))
        {
            try
            {
                openTargetDataLine();
            }
            catch(LineUnavailableException e)
            {
                mLog.error("Unable to open mixer source target data line", e);
                mRunning.set(false);
                return;
            }

            //Cancel the scheduled thread if it wasn't cancelled previously ... this shouldn't happen
            if(mScheduledFuture != null)
            {
                mScheduledFuture.cancel(true);
                mScheduledFuture = null;
            }

            mScheduledFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this,
                0, BUFFER_PROCESSING_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }
        else
        {
            mLog.warn("Attempt to start an already running BufferReader instance - this shouldn't happen");
        }
    }

    /**
     * Stops the sample processing reader thread
     */
    public void stop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            if(mScheduledFuture != null)
            {
                mScheduledFuture.cancel(true);
                mScheduledFuture = null;
            }

            closeTargetDataLine();
        }
        else
        {
            mLog.warn("Attempt to stop an already stopped BufferReader instance - this shouldn't happen");
        }
    }

    /**
     * Sample processing thread.  This method runs each time the timer fires.
     */
    @Override
    public void run()
    {
        if(mRunning.get())
        {
            if(mHeartbeatManager != null)
            {
                mHeartbeatManager.broadcast();
            }

            try
            {
                mBytesRead = 0;

                //Blocking read - waits until the buffer fills
                mBytesRead = mTargetDataLine.read(mBuffer, 0, mBuffer.length);

                if(mBytesRead == mBuffer.length)
                {
                    //Sample adapter automatically sets initial listener count to one.
                    T reusableBuffer = mSampleAdapter.convert(mBuffer);

                    if(reusableBuffer != null && mReusableBufferListener != null)
                    {
                        mReusableBufferListener.receive(reusableBuffer);
                    }
                }
                else if(mBytesRead > 0)
                {
                    //Sample adapter automatically sets initial listener count to one.
                    T reusableBuffer = mSampleAdapter.convert(Arrays.copyOf(mBuffer, mBytesRead));

                    if(reusableBuffer != null && mReusableBufferListener != null)
                    {
                        mReusableBufferListener.receive(reusableBuffer);
                    }
                }
            }
            catch(Exception e)
            {
                mLog.error("MixerSource - error while reading from the mixer target data line", e);
                stop();
            }
        }
    }

    /**
     * Indicates if the reader thread is running
     */
    public boolean isRunning()
    {
        return mRunning.get();
    }

    /**
     * Sets the listener to receive sample data in reusable buffers.
     */
    public void setBufferListener(Listener<T> listener)
    {
        mReusableBufferListener = listener;
    }

    /**
     * Removes the listener from receiving sample data.
     */
    public void removeBufferListener()
    {
        mReusableBufferListener = null;
    }

    /**
     * Registers a listener to receive source events
     *
     * @param listener
     */
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mSourceEventListener = listener;
    }

    /**
     * Removes a listener from receiving source events
     */
    public void removeSourceEventListener()
    {
        mSourceEventListener = null;
    }

    /**
     * Current source event listener
     */
    public Listener<SourceEvent> getSourceEventListener()
    {
        return mSourceEventListener;
    }

    /**
     * Sample rate specified for the underlying target data line.
     */
    public double getSampleRate()
    {
        return mAudioFormat.getSampleRate();
    }

    public void dispose()
    {
        stop();
        mHeartbeatManager = null;
        mReusableBufferListener = null;
        mSourceEventListener = null;
    }
}
