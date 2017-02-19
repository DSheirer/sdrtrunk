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
package dsp.mixer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class LowPhaseNoiseOscillator
{
    private final static Logger mLog = LoggerFactory.getLogger(LowPhaseNoiseOscillator.class);

    private static final double TWO_PI = 2.0 * Math.PI;
    private static final float THREE_HALVES = 3.0f / 2.0f;
    private int mSampleRate;
    private int mFrequency;
    private float mInphase = 1.0f;
    private float mQuadrature = 0.0f;
    private float mPreviousInphase = 1.0f;
    private float mPreviousQuadrature = 0.0f;
    private float mCosineAngle;
    private float mSineAngle;
    private float mGain = 1.0f;

    /**
     * Ultra-low phase noise complex oscillator as described in Digital Signal Processing 3e, Lyons, p.786
     *
     * @param sampleRate for the oscillator
     * @param frequency to generate
     */
    public LowPhaseNoiseOscillator(int sampleRate, int frequency)
    {
        mSampleRate = sampleRate;
        mFrequency = frequency;

        update();
    }

    public void setSampleRate(int sampleRate)
    {
        mSampleRate = sampleRate;
        update();
    }

    public void setFrequency(int frequency)
    {
        mFrequency = frequency;
        update();
    }

    /**
     * Updates the internal values after a frequency or sample rate change
     */
    private void update()
    {
        double anglePerSample = TWO_PI * mFrequency / mSampleRate;

        mCosineAngle = (float)Math.cos(anglePerSample);
        mSineAngle = (float)Math.sin(anglePerSample);
    }

    private void rotate()
    {
        mInphase = ((mPreviousInphase * mCosineAngle) - (mPreviousQuadrature * mSineAngle)) * mGain;
        mQuadrature = ((mPreviousInphase * mSineAngle) + (mPreviousQuadrature * mCosineAngle)) * mGain;

        mPreviousInphase = mInphase;
        mPreviousQuadrature = mQuadrature;

        mGain = THREE_HALVES - ((mPreviousInphase * mPreviousInphase) + (mPreviousQuadrature * mPreviousQuadrature));
    }

    /**
     * Generates an array of samples with length of 2 x sample count containing interleaved inphase and quadrature
     * samples.
     *
     * @param sampleCount number of samples to generate
     * @return array that is sampleCount * 2 in length
     */
    public float[] generate(int sampleCount)
    {
        float[] samples = new float[sampleCount * 2];

        for(int x = 0; x < sampleCount; x++)
        {
            int index = x * 2;
            samples[index] = mInphase;
            samples[index + 1] = mQuadrature;
            rotate();
        }

        return samples;
    }

    public static void main(String[] args)
    {
        LowPhaseNoiseOscillator o = new LowPhaseNoiseOscillator(10, 1);

        float[] samples = o.generate(20);

        StringBuilder sb = new StringBuilder();
        for(int x = 0; x < samples.length; x += 2)
        {
            sb.append(samples[x]).append(" ").append(samples[x + 1]).append("\n");
        }

        mLog.debug("Samples:\n" + sb.toString());
    }
}
