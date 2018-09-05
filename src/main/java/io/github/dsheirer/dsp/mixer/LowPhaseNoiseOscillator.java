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
package io.github.dsheirer.dsp.mixer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class LowPhaseNoiseOscillator extends AbstractOscillator
{
    private final static Logger mLog = LoggerFactory.getLogger(LowPhaseNoiseOscillator.class);

    private static final double TWO_PI = 2.0 * Math.PI;
    private static final double THREE_HALVES = 3.0 / 2.0;
    private double mInphase = 1.0;
    private double mQuadrature = 0.0;
    private double mPreviousInphase = 1.0;
    private double mPreviousQuadrature = 0.0;
    private double mCosineAngle;
    private double mSineAngle;
    private double mGain = 1.0;

    /**
     * Ultra-low phase noise complex oscillator as described in Digital Signal Processing 3e, Lyons, p.786
     *
     * @param sampleRate for the oscillator
     * @param frequency to generate
     */
    public LowPhaseNoiseOscillator(double frequency, double sampleRate)
    {
        super(frequency, sampleRate);

        update();
    }

    @Override
    public float quadrature()
    {
        return (float)mQuadrature;
    }

    @Override
    public float inphase()
    {
        return (float)mInphase;
    }

    /**
     * Updates the internal values after a frequency or sample rate change
     */
    protected void update()
    {
        double anglePerSample = TWO_PI * getFrequency() / getSampleRate();

        mCosineAngle = Math.cos(anglePerSample);
        mSineAngle = Math.sin(anglePerSample);
    }

    /**
     * Rotates the oscillator to generate the next complex sample.
     */
    public void rotate()
    {
        mInphase = ((mPreviousInphase * mCosineAngle) - (mPreviousQuadrature * mSineAngle)) * mGain;
        mQuadrature = ((mPreviousInphase * mSineAngle) + (mPreviousQuadrature * mCosineAngle)) * mGain;

        mPreviousInphase = mInphase;
        mPreviousQuadrature = mQuadrature;

        //Update the gain value for the next rotation
        mGain = THREE_HALVES - ((mPreviousInphase * mPreviousInphase) + (mPreviousQuadrature * mPreviousQuadrature));
    }

    public static long process(AbstractOscillator oscillator, int iterations, int sampleCount)
    {
        long start = System.nanoTime();

        for(int x = 0; x < iterations; x++)
        {
            float[] samples = oscillator.generateComplex(sampleCount);
        }

        long duration = System.nanoTime() - start;

        return duration;
    }

    public static void main(String[] args)
    {
        mLog.debug("Starting ...");
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss.SSS");

        AbstractOscillator lpno = new LowPhaseNoiseOscillator(100, 800);
        AbstractOscillator regular = new Oscillator(100, 800);

        int iterations = 10000;
        int sampleCount = 1000000;

        mLog.debug("Warm up - lpno");
        process(lpno, iterations, sampleCount);
        process(lpno, iterations, sampleCount);
        process(lpno, iterations, sampleCount);

        mLog.debug("Warm up - regular");
        process(regular, iterations, sampleCount);
        process(regular, iterations, sampleCount);
        process(regular, iterations, sampleCount);

        mLog.debug("Test - lpno");
        process(lpno, iterations, sampleCount);
        process(lpno, iterations, sampleCount);
        long duration = process(lpno, iterations, sampleCount);
        mLog.debug("Duration Low Phase Noise: " + sdf.format(new Date(TimeUnit.NANOSECONDS.toMillis(duration))));

        mLog.debug("Test - regular");
        process(regular, iterations, sampleCount);
        process(regular, iterations, sampleCount);
        duration = process(regular, iterations, sampleCount);
        mLog.debug("Duration         Regular: " + sdf.format(new Date(TimeUnit.NANOSECONDS.toMillis(duration))));

//        mLog.debug("                   Delta: " + sdf.format(new Date(TimeUnit.NANOSECONDS.toMillis(duration2 - duration))));
    }
}
