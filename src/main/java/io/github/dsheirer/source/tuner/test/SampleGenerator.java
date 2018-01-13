/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.source.tuner.test;

import io.github.dsheirer.dsp.mixer.LowPhaseNoiseOscillator;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexBuffer;
import io.github.dsheirer.sample.complex.TimestampedComplexBuffer;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SampleGenerator
{
    private final static Logger mLog = LoggerFactory.getLogger(SampleGenerator.class);

    private LowPhaseNoiseOscillator mOscillator;
    private int mSweepUpdateInterval;
    private Listener<ComplexBuffer> mListener;
    private long mInterval;
    private int mSamplesPerInterval;
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
        if(Math.abs(sweepUpdateRate) >= sampleRate)
        {
            throw new IllegalArgumentException("Sweep update rate cannot be greater than sample rate");
        }

        mLog.debug("Sample Generator - Sample Rate:" + sampleRate + " Frequency:" + frequency);
        mOscillator = new LowPhaseNoiseOscillator(sampleRate, frequency);
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

        mLog.debug("Sample Generator - " + mSamplesPerInterval + " samples every " + mInterval + "ms");
    }

    /**
     * Starts the generator producing samples
     */
    public void start()
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
    public void stop()
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
     * Registers the listener to receive samples once the generator starts.  Note: invoke this method with null
     * argument to de-register the listener.
     *
     * @param listener to receive complex sample buffers
     */
    public void setListener(Listener<ComplexBuffer> listener)
    {
        mListener = listener;
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
        return mOscillator.getFrequency();
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

    /**
     * Generates a complex sample buffer and distributes the buffer to a registered listener
     */
    public class Generator implements Runnable
    {
        @Override
        public void run()
        {
            if(mListener != null)
            {
                float[] samples = mOscillator.generate(mSamplesPerInterval);
                mListener.receive(new TimestampedComplexBuffer(samples));

                if(mSweepUpdateInterval != 0)
                {
                    long updatedFrequency = mOscillator.getFrequency() + mSweepUpdateInterval;

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
