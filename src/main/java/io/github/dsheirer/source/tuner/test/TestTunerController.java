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

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestTunerController extends TunerController
{
    private final static Logger mLog = LoggerFactory.getLogger(TestTunerController.class);

    public static final long MINIMUM_FREQUENCY = 1l;
    public static final long MAXIMUM_FREQUENCY = 1000000000l;
    public static final int SAMPLE_RATE = 50000;
    public static final int DC_NOISE_BANDWIDTH = 0;
    public static final double USABLE_BANDWIDTH_PERCENTAGE = 1.00;

    public static final int SPECTRAL_FRAME_RATE = 20;
    public static final long SAMPLE_GENERATION_INTERVAL = 1000 / SPECTRAL_FRAME_RATE;

    private SampleGenerator mSampleGenerator;
    private long mFrequency = 100000000l;

    /**
     * Tuner controller testing implementation.
      */
    public TestTunerController()
    {
        super(MINIMUM_FREQUENCY, MAXIMUM_FREQUENCY, DC_NOISE_BANDWIDTH, USABLE_BANDWIDTH_PERCENTAGE);

        int sweepRate = 0;  //Hz per interval
        long initialToneFrequency = SAMPLE_RATE / 2 + 100;
        mSampleGenerator = new SampleGenerator(SAMPLE_RATE, initialToneFrequency, SAMPLE_GENERATION_INTERVAL,
            sweepRate);

        try
        {
            mFrequencyController.setFrequency(mFrequency);
            mFrequencyController.setSampleRate(SAMPLE_RATE);
        }
        catch(Exception e)
        {
            mLog.error("Error!", e);
        }
    }

    @Override
    public int getBufferSampleCount()
    {
        return SAMPLE_RATE / SPECTRAL_FRAME_RATE;
    }

    @Override
    public void dispose()
    {
        //no-op
    }

    @Override
    public void addBufferListener(Listener<ReusableComplexBuffer> listener)
    {
        mSampleGenerator.addListener(listener);
    }

    @Override
    public void removeBufferListener(Listener<ReusableComplexBuffer> listener)
    {
        mSampleGenerator.removeListener(listener);
    }

    @Override
    public void apply(TunerConfiguration config) throws SourceException
    {
        mLog.error("Request to apply tuner configuration was ignored");
    }

    /**
     * Current center frequency for this tuner
     * @throws SourceException
     */
    @Override
    public long getTunedFrequency() throws SourceException
    {
        return mFrequency;
    }

    /**
     * Sets the center frequency for this tuner
     * @param frequency in hertz
     * @throws SourceException
     */
    @Override
    public void setTunedFrequency(long frequency) throws SourceException
    {
        mFrequency = frequency;
    }

    /**
     * Sets the tone output of the frequency generator
     * @param frequency in the range: 0 <> Sample Rate
     */
    public void setToneFrequency(long frequency)
    {
        mSampleGenerator.setFrequency(frequency);
    }

    /**
     * Frequency of the tone being generated
     *
     * @return tone frequency in range: 0 <> Sample Rate
     */
    public long getToneFrequency()
    {
        return mSampleGenerator.getFrequency();
    }

    /**
     * Current sample rate for this tuner controller
     */
    @Override
    public double getCurrentSampleRate()
    {
        return mSampleGenerator.getSampleRate();
    }

    /**
     * Sets the sample rate for this tuner controller
     */
    public void setSampleRate(int sampleRate) throws SourceException
    {
        mSampleGenerator.setSampleRate(sampleRate);
        mFrequencyController.setSampleRate(sampleRate);
    }
}
