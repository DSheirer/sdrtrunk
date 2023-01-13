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
package io.github.dsheirer.source.tuner.test;

import io.github.dsheirer.buffer.FloatNativeBuffer;
import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.dsp.oscillator.AWGNOscillator;
import io.github.dsheirer.dsp.oscillator.IComplexOscillator;
import io.github.dsheirer.dsp.oscillator.OscillatorFactory;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.ThreadPool;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleGenerator
{
    private final static Logger mLog = LoggerFactory.getLogger(SampleGenerator.class);

    private Broadcaster<INativeBuffer> mNativeBufferBroadcaster = new Broadcaster<>();
    private IComplexOscillator mComplexOscillator;
    private int mSweepUpdateInterval;
    private long mInterval;
    private int mSamplesPerInterval = 65536;
    private ScheduledFuture<?> mScheduledFuture;

    /**
     * Generates complex sample buffers at the specified sample rate with a unity gain tone at the specified
     * frequency.  This generator runs via a scheduled thread pool and generates samples at the specified time
     * interval.
     *
     * @param sampleRate of the complex samples
     * @param frequency of the tone produced by the generator
     * @param sweepUpdateRate to turn on sweeping of the tone frequency where the frequency is incremented by the
     * rate specified at each interval, resetting to 1 Hz after reaching/exceeding the highest frequency.
     */
    public SampleGenerator(int sampleRate, long frequency, int sweepUpdateRate)
    {
        if(FastMath.abs(sweepUpdateRate) >= sampleRate)
        {
            throw new IllegalArgumentException("Sweep update rate cannot be greater than sample rate");
        }

//        mComplexOscillator = OscillatorFactory.getComplexOscillator(frequency, sampleRate);
        IComplexOscillator oscillator = OscillatorFactory.getComplexOscillator(frequency, sampleRate);
        mComplexOscillator = new AWGNOscillator(oscillator, 0.003f);

        mInterval = 1000 / (sampleRate / mSamplesPerInterval);
        mSweepUpdateInterval = sweepUpdateRate;
    }

    /**
     * Generates complex sample buffers at the specified sample rate with a unity gain tone at the specified
     * frequency.  This generator runs via a scheduled thread pool and generates samples at the specified time
     * interval.
     *
     * @param sampleRate of the complex samples
     * @param frequency of the tone produced by the generator
     */
    public SampleGenerator(int sampleRate, long frequency)
    {
        this(sampleRate, frequency, 0);
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
    public void addListener(Listener<INativeBuffer> listener)
    {
        mNativeBufferBroadcaster.addListener(listener);

        if(mNativeBufferBroadcaster.getListenerCount() == 1)
        {
            start();
        }
    }

    /**
     * Removes the listener and stops the sample generator if there are no more listeners.
     */
    public void removeListener(Listener<INativeBuffer> listener)
    {
        mNativeBufferBroadcaster.removeListener(listener);

        if(mNativeBufferBroadcaster.getListenerCount() == 0)
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
        mComplexOscillator.setFrequency(frequency);
    }

    /**
     * Current tone frequency for this generator
     */
    public long getFrequency()
    {
        return (long) mComplexOscillator.getFrequency();
    }

    /**
     * Updates the sample rate for this generator
     * @param sampleRate in hertz for complex samples
     */
    public void setSampleRate(int sampleRate)
    {
        mComplexOscillator.setSampleRate(sampleRate);
        mInterval = 1000 / (sampleRate / mSamplesPerInterval);
    }

    public double getSampleRate()
    {
        return mComplexOscillator.getSampleRate();
    }

    /**
     * Generates a complex sample buffer and distributes the buffer to a registered listener
     */
    public class Generator implements Runnable
    {
        private int mTriggerInterval = 0;

        @Override
        public void run()
        {
            try
            {
                if(mNativeBufferBroadcaster.hasListeners())
                {
                    float[] samples = mComplexOscillator.generate(mSamplesPerInterval);

                    long now = System.currentTimeMillis();

                    FloatNativeBuffer buffer = new FloatNativeBuffer(samples, now, 0.0f);

                    mNativeBufferBroadcaster.broadcast(buffer);

                    if(mSweepUpdateInterval != 0)
                    {
                        mTriggerInterval++;

                        if(mTriggerInterval >= 10)
                        {
                            mTriggerInterval = 0;

                            long updatedFrequency = (long) mComplexOscillator.getFrequency() + mSweepUpdateInterval;

                            if(updatedFrequency > mComplexOscillator.getSampleRate() / 2)
                            {
                                mComplexOscillator.setFrequency(mComplexOscillator.getSampleRate() / -2);
                            }
                            else if(updatedFrequency < mComplexOscillator.getSampleRate() / -2)
                            {
                                mComplexOscillator.setFrequency(mComplexOscillator.getSampleRate() / 2);
                            }
                            else
                            {
                                mComplexOscillator.setFrequency(updatedFrequency);
                            }
                        }
                    }
                }
            }
            catch(Throwable t)
            {
                t.printStackTrace();
            }
        }
    }
}
