/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.source.mixer;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.adapter.ISampleAdapter;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.heartbeat.HeartbeatManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Target Data Line sample data processor.  Performs blocking read against the mixer target data line, converts
 * the samples to an array of floats using the specified adapter.  Dispatches float arrays to the registered
 * buffer listener.
 */
public class MixerReader<T>
{
    private final static Logger mLog = LoggerFactory.getLogger(MixerReader.class);
    private TargetDataLine mTargetDataLine;
    private int mBufferSize;
    private AtomicBoolean mRunning = new AtomicBoolean();
    private ISampleAdapter<T> mSampleAdapter;
    private Listener<T> mListener;
    private Listener<SourceEvent> mSourceEventListener;
    private AudioFormat mAudioFormat;
    private HeartbeatManager mHeartbeatManager;
    private Thread mReaderThread;

    /**
     * Constructs an instance
     * @param audioFormat for the target mixer
     * @param targetDataLine to read samples from
     * @param abstractSampleAdapter to convert samples to native buffers
     * @param heartbeatManager to ping before each buffer read for downstream consumers
     */
    public MixerReader(AudioFormat audioFormat, TargetDataLine targetDataLine,
                       ISampleAdapter<T> abstractSampleAdapter, HeartbeatManager heartbeatManager)
    {
        mTargetDataLine = targetDataLine;
        mAudioFormat = audioFormat;
        mSampleAdapter = abstractSampleAdapter;
        mHeartbeatManager = heartbeatManager;

        if(mAudioFormat.getSampleRate() <= 96000)
        {
            setBufferSampleSize(4096);
        }
        else
        {
            setBufferSampleSize(8192);
        }
    }

    /**
     * Constructs an instance
     * @param audioFormat for the target mixer
     * @param targetDataLine to read samples from
     * @param abstractSampleAdapter to convert samples to native buffers
     */
    public MixerReader(AudioFormat audioFormat, TargetDataLine targetDataLine, ISampleAdapter<T> abstractSampleAdapter)
    {
        this(audioFormat, targetDataLine, abstractSampleAdapter, new HeartbeatManager());
    }

    /**
     * Audio format used by this reader
     */
    public AudioFormat getAudioFormat()
    {
        return mAudioFormat;
    }

    /**
     * Sets the number of samples per native buffer to produce.
     * @param samples is the number of samples
     */
    public void setBufferSampleSize(int samples)
    {
        mBufferSize = samples * mAudioFormat.getFrameSize();
    }

    /**
     * Starts the sample processing reader thread
     */
    public void start()
    {
        if(mRunning.compareAndSet(false, true))
        {
            if(mTargetDataLine == null)
            {
                mRunning.set(false);
                mLog.error("Attempt to start failed - target data line is null");
                return;
            }

            try
            {
                mTargetDataLine.open(getAudioFormat());
                mTargetDataLine.start();
            }
            catch(LineUnavailableException e)
            {
                mLog.error("Unable to open mixer source target data line", e);
                mRunning.set(false);
                return;
            }

            mReaderThread = new Thread(new DataLineReader());
            mReaderThread.setName("sdrtrunk mixer sample reader");
            mReaderThread.start();
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
            if(mTargetDataLine != null && mTargetDataLine.isOpen())
            {
                if(mTargetDataLine.isRunning())
                {
                    mTargetDataLine.stop();
                }

                if(mTargetDataLine.isOpen())
                {
                    mTargetDataLine.close();
                }
            }

            mReaderThread.interrupt();

            try
            {
                mReaderThread.join(500);
            }
            catch(InterruptedException ie)
            {
                //No-op ... we're shutting down
            }

            mReaderThread = null;
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
        mListener = listener;
    }

    /**
     * Removes the listener from receiving sample data.
     */
    public void removeBufferListener()
    {
        mListener = null;
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
        mHeartbeatManager = null;
        mListener = null;
        mSourceEventListener = null;
    }

    /**
     * Runnable reader to read samples from the target data line, convert them to native buffers, and dispatch.
     */
    public class DataLineReader implements Runnable
    {
        @Override
        public void run()
        {
            while(mRunning.get())
            {
                if(mHeartbeatManager != null)
                {
                    mHeartbeatManager.broadcast();
                }

                byte[] buffer = new byte[mBufferSize];

                int bytesRead = 0;

                try
                {
                    while(bytesRead < buffer.length)
                    {
                        bytesRead += mTargetDataLine.read(buffer, bytesRead, buffer.length);
                    }
                }
                catch(ArrayIndexOutOfBoundsException aioobe)
                {
                    //No-op ... this can happen during USB disconnect
                }

                //We'll always read the correct number of bytes until the data line is closed or stopped, and we're
                //shutting down.
                if(bytesRead == buffer.length && mListener != null)
                {
                    mListener.receive(mSampleAdapter.convert(buffer));
                }
            }
        }
    }
}
