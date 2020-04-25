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
package io.github.dsheirer.source.tuner.test;

import io.github.dsheirer.dsp.mixer.IOscillator;
import io.github.dsheirer.dsp.mixer.LowPhaseNoiseOscillator;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableBufferBroadcaster;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBufferQueue;
import io.github.dsheirer.util.ThreadPool;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SampleGenerator
{
    private final static Logger mLog = LoggerFactory.getLogger(SampleGenerator.class);

    private ReusableBufferBroadcaster mComplexBufferBroadcaster = new ReusableBufferBroadcaster();
    private IOscillator mOscillator;
    private int mSweepUpdateInterval;
    private long mInterval;
    private int mSamplesPerInterval;
    private boolean mReuseBuffers;
    private ScheduledFuture<?> mScheduledFuture;

    /**
     * Generates complex sample buffers at the specified sample rate with a unity gain tone at the specified
     * frequency.  This generator runs via a scheduled thread pool and generates samples at the specified time
     * interval.
     *
     * @param sampleRate of the complex samples
     * @param frequency of the tone produced by the generator
     * @param interval in milliseconds for generating samples
     * @param sweepUpdateRate to turn on sweeping of the tone frequency where the frequency is incremented by the
     * rate specified at each interval, resetting to 1 Hz after reaching/exceeding the highest frequency.
     */
    public SampleGenerator(int sampleRate, long frequency, long interval, int sweepUpdateRate)
    {
        if(FastMath.abs(sweepUpdateRate) >= sampleRate)
        {
            throw new IllegalArgumentException("Sweep update rate cannot be greater than sample rate");
        }

        mOscillator = new LowPhaseNoiseOscillator(frequency, sampleRate);
        mInterval = interval;
        mSweepUpdateInterval = sweepUpdateRate;

        updateSamplesPerInterval();
    }

    /**
     * Generates complex sample buffers at the specified sample rate with a unity gain tone at the specified
     * frequency.  This generator runs via a scheduled thread pool and generates samples at the specified time
     * interval.
     *
     * @param sampleRate of the complex samples
     * @param frequency of the tone produced by the generator
     * @param interval in milliseconds for generating samples
     */
    public SampleGenerator(int sampleRate, long frequency, long interval)
    {
        this(sampleRate, frequency, interval, 0);
    }

    /**
     * Updates the number of complex samples generated per interval
     */
    private void updateSamplesPerInterval()
    {
        mSamplesPerInterval = (int)((double)mOscillator.getSampleRate() * ((double)mInterval / 1000.0));
    }

    /**
     * Starts the generator producing samples
     */
    private void start()
    {
        if(mScheduledFuture == null)
        {
            mScheduledFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(new Generator(), 0, mInterval,
                TimeUnit.MILLISECONDS);
        }
        else
        {
            throw new IllegalStateException("Sample generator is already started");
        }
    }

    /**
     * Stops the generator from producing samples
     */
    private void stop()
    {
        if(mScheduledFuture != null)
        {
            mScheduledFuture.cancel(true);
            mScheduledFuture = null;
        }
        else
        {
            throw new IllegalStateException("Sample generator is already stopped");
        }
    }

    /**
     * Registers the listener to receive samples and auto-starts the generator if this is the first listener.
     *
     * @param listener to receive complex sample buffers
     */
    public void addListener(Listener<ReusableComplexBuffer> listener)
    {
        mComplexBufferBroadcaster.addListener(listener);

        if(mComplexBufferBroadcaster.getListenerCount() == 1)
        {
            start();
        }
    }

    /**
     * Removes the listener and stops the sample generator if there are no more listeners.
     */
    public void removeListener(Listener<ReusableComplexBuffer> listener)
    {
        mComplexBufferBroadcaster.removeListener(listener);

        if(mComplexBufferBroadcaster.getListenerCount() == 0)
        {
            stop();
        }
    }

    /**
     * Updates the tone frequency for this generator
     * @param frequency to generate
     */
    public void setFrequency(long frequency)
    {
        mOscillator.setFrequency(frequency);
    }

    /**
     * Current tone frequency for this generator
     */
    public long getFrequency()
    {
        return (long)mOscillator.getFrequency();
    }

    /**
     * Updates the sample rate for this generator
     * @param sampleRate in hertz for complex samples
     */
    public void setSampleRate(int sampleRate)
    {
        mOscillator.setSampleRate(sampleRate);
        updateSamplesPerInterval();
    }

    public double getSampleRate()
    {
        return mOscillator.getSampleRate();
    }

    /**
     * Generates a complex sample buffer and distributes the buffer to a registered listener
     */
    public class Generator implements Runnable
    {
        private int mTriggerInterval = 0;
        private ReusableComplexBufferQueue mReusableComplexBufferQueue = new ReusableComplexBufferQueue("SampleGenerator");

        @Override
        public void run()
        {
            if(mComplexBufferBroadcaster.hasListeners())
            {
                ReusableComplexBuffer reusableComplexBuffer = mReusableComplexBufferQueue.getBuffer(mSamplesPerInterval);

                mOscillator.generateComplex(reusableComplexBuffer);

                mComplexBufferBroadcaster.broadcast(reusableComplexBuffer);

                if(mSweepUpdateInterval != 0)
                {
                    mTriggerInterval++;

                    if(mTriggerInterval >= 10)
                    {
                        mTriggerInterval = 0;

                        long updatedFrequency = (long)mOscillator.getFrequency() + mSweepUpdateInterval;

                        if(updatedFrequency > mOscillator.getSampleRate() / 2)
                        {
                            mOscillator.setFrequency(mOscillator.getSampleRate() / -2);
                        }
                        else if(updatedFrequency < mOscillator.getSampleRate() / -2)
                        {
                            mOscillator.setFrequency(mOscillator.getSampleRate() / 2);
                        }
                        else
                        {
                            mOscillator.setFrequency(updatedFrequency);
                        }
                    }
                }
            }
        }
    }
}
