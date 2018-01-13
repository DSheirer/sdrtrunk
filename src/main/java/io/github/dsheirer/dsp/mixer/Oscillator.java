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
package io.github.dsheirer.dsp.mixer;

import io.github.dsheirer.sample.complex.Complex;
import io.github.dsheirer.sample.complex.ComplexBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Oscillator
{
    private final static Logger mLog = LoggerFactory.getLogger(Oscillator.class);

    private boolean mEnabled = true;
    private double mFrequency;
    private double mSampleRate;

    private Complex mAnglePerSample;
    private Complex mCurrentAngle = new Complex(0.0f, -1.0f);

    /**
     * Oscillator produces complex or float samples corresponding to a sine wave
     * oscillating at the specified frequency and sample rate
     *
     * @param frequency - positive or negative frequency in hertz
     * @param sampleRate - in hertz
     */
    public Oscillator(long frequency, double sampleRate)
    {
        mEnabled = frequency != 0;

        mSampleRate = sampleRate;
        mFrequency = (double)frequency;

        update();
    }

    /**
     * Indicates if this oscillator has a non-zero frequency setting.  When used as a frequency translating
     * mixer, setting a frequency of zero indicates that no frequency correction is needed, so this flag can
     * indicate when to apply the oscillator to frequency correct samples.
     *
     * @return true if the current frequency value is non-zero.
     */
    public boolean enabled()
    {
        return mEnabled;
    }

    /**
     * Updates the internal values after a frequency or sample rate change
     */
    private void update()
    {
        float anglePerSample =
            (float)(2.0d * Math.PI * mFrequency / mSampleRate);

        mAnglePerSample = Complex.fromAngle(anglePerSample);
    }

    /**
     * Sets or changes the frequency of this oscillator
     */
    public void setFrequency(long frequency)
    {
        mEnabled = frequency != 0;
        mFrequency = (double)frequency;
        update();
    }

    /**
     * Sets or changes the sample rate of this oscillator
     */
    public void setSampleRate(double sampleRate)
    {
        mSampleRate = sampleRate;
        update();
    }

    /**
     * Steps the current angle by the angle per sample amount
     */
    public void rotate()
    {
        mCurrentAngle.multiply(mAnglePerSample);
        mCurrentAngle.fastNormalize();
    }

    public float inphase()
    {
        return mCurrentAngle.inphase();
    }

    public float quadrature()
    {
        return mCurrentAngle.quadrature();
    }

    /**
     * Get next complex sample
     */
    public Complex getComplex()
    {
        return mCurrentAngle.copy();
    }

    /**
     * Get the next float sample
     */
    public float getFloat()
    {
        return mCurrentAngle.real();
    }

    /**
     * Generates the requested number of complex samples and returns them in a complex buffer
     *
     * @param sampleCount to generate
     * @return complex buffer containing the requested complex samples
     */
    public ComplexBuffer generateComplexBuffer(int sampleCount)
    {
        float[] buffer = new float[sampleCount * 2];

        for(int x = 0; x < sampleCount; x++)
        {
            int index = 2 * x;
            buffer[index] = inphase();
            buffer[index + 1] = quadrature();

            rotate();
        }

        return new ComplexBuffer(buffer);
    }

    /**
     * Performs complex heterodyne against the samples using this oscillator
     * @param samples to mix with this oscillator
     */
    public void mixComplex(float[] samples)
    {
        for(int x = 0; x < samples.length; x += 2)
        {
            float i = Complex.multiplyInphase(samples[x], samples[x + 1], inphase(), quadrature());
            float q = Complex.multiplyQuadrature(samples[x], samples[x + 1], inphase(), quadrature());

            samples[x] = i;
            samples[x + 1] = q;

            rotate();
        }
    }
}
