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
package source.tuner.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;
import sample.complex.ComplexBuffer;
import source.SourceException;
import source.tuner.TunerController;
import source.tuner.configuration.TunerConfiguration;

public class TestTunerController extends TunerController
{
    private final static Logger mLog = LoggerFactory.getLogger(TestTunerController.class);

    public static final long MINIMUM_FREQUENCY = 1000000l;     //1 MHz
    public static final long MAXIMUM_FREQUENCY = 10000000000l; //10 GHz
    public static final long DEFAULT_CENTER_FREQUENCY = 100000000;    //100 MHz
    public static final int DEFAULT_SAMPLE_RATE = 50000;     //25 kHz
    public static final int DC_NOISE_BANDWIDTH = 5000;         // +/-5 kHz
    public static final double USABLE_BANDWIDTH = 0.90;  //90% usable bandwidth - 5% unusable on either end
    public static final int SPECTRAL_FRAME_RATE = 20;
    public static final long SAMPLE_GENERATION_INTERVAL = 1000 / SPECTRAL_FRAME_RATE;

    private SampleGenerator mSampleGenerator;
    private long mReferenceFrequency = DEFAULT_CENTER_FREQUENCY;

    /**
     * Tuner controller testing implementation.
      */
    public TestTunerController()
    {
        super(MINIMUM_FREQUENCY, MAXIMUM_FREQUENCY, DC_NOISE_BANDWIDTH, USABLE_BANDWIDTH);

        int sweepRate = 0;  //Hz per interval

        mSampleGenerator = new SampleGenerator(DEFAULT_SAMPLE_RATE, 12500, SAMPLE_GENERATION_INTERVAL, sweepRate);

        try
        {
            mLog.debug("Setting frequency ...");
            mFrequencyController.setFrequency(DEFAULT_CENTER_FREQUENCY);
            mLog.debug("Setting sample rate of freq controller to: " + DEFAULT_SAMPLE_RATE);
            mFrequencyController.setSampleRate(DEFAULT_SAMPLE_RATE);
        }
        catch(Exception e)
        {
            mLog.error("Error!", e);
        }
    }

    /**
     * Starts the tuner controller producing samples that will be delivered to the specified listener
     */
    public void start(Listener<ComplexBuffer> listener)
    {
        mSampleGenerator.setListener(listener);
        mSampleGenerator.start();
    }

    /**
     * Stops the tuner controller sample generator and deregisters the listener
     */
    public void stop()
    {
        mSampleGenerator.stop();
        mSampleGenerator.setListener(null);
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
        return mReferenceFrequency;
    }

    /**
     * Sets the center frequency for this tuner
     * @param frequency in hertz
     * @throws SourceException
     */
    @Override
    public void setTunedFrequency(long frequency) throws SourceException
    {
        mReferenceFrequency = frequency;
    }

    /**
     * Sets the tone frequency for the sample generator.  The tone frequency is offset from the center tuned frequency.
     * @param toneFrequency
     */
    public void setToneFrequency(long toneFrequency)
    {
        long frequency = toneFrequency - mReferenceFrequency;
        mSampleGenerator.setFrequency(frequency);
    }

    /**
     * Current tone frequency for this test controller.
     * @return
     */
    public long getToneFrequency()
    {
        return mReferenceFrequency + mSampleGenerator.getFrequency();
    }

    /**
     * Current sample rate for this tuner controller
     */
    @Override
    public int getCurrentSampleRate() throws SourceException
    {
        return mFrequencyController.getSampleRate();
    }

    /**
     * Sets the sample rate for this tuner controller
     */
    public void setSampleRate(int sampleRate) throws SourceException
    {
        mFrequencyController.setSampleRate(sampleRate);
    }
}
