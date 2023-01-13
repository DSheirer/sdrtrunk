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

package io.github.dsheirer.dsp.mixer;

import io.github.dsheirer.dsp.oscillator.IComplexOscillator;
import io.github.dsheirer.dsp.oscillator.OscillatorFactory;
import io.github.dsheirer.dsp.oscillator.ScalarComplexOscillator;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;

/**
 * Base complex mixer that wraps an oscillator implementation and mixes complex sample buffers
 */
public abstract class ComplexMixer
{
    private IComplexOscillator mOscillator;

    /**
     * Constructs an instance
     * @param frequency to generate
     * @param sampleRate to generate
     * @param disableSIMD to disable use of SIMD vector instructions.
     */
    public ComplexMixer(double frequency, double sampleRate, boolean disableSIMD)
    {
        if(disableSIMD)
        {
            mOscillator = new ScalarComplexOscillator(frequency, sampleRate);
        }
        else
        {
            mOscillator = OscillatorFactory.getComplexOscillator(frequency, sampleRate);
        }
    }

    public ComplexMixer(double frequency, double sampleRate)
    {
        mOscillator = OscillatorFactory.getComplexOscillator(frequency, sampleRate);
    }

    /**
     * Sets the frequency of the underlying oscillator
     * @param frequency in Hertz
     */
    public void setFrequency(double frequency)
    {
        mOscillator.setFrequency(frequency);
    }

    /**
     * Indicates if this mixer's oscillator is set to a non-zero frequency value.
     */
    public boolean hasFrequency()
    {
        return mOscillator.hasFrequency();
    }

    /**
     * Current frequency of this oscillator
     * @return frequency in hertz
     */
    public double getFrequency()
    {
        return mOscillator.getFrequency();
    }

    /**
     * Sets the sample rate of the underlying oscillator
     * @param sampleRate in Hertz
     */
    public void setSampleRate(double sampleRate)
    {
        mOscillator.setSampleRate(sampleRate);
    }

    /**
     * Generates complex samples from the underlying oscillator
     * @param sampleCount to generate
     * @param timestamp of the first sample
     * @return complex samples
     */
    protected ComplexSamples generate(int sampleCount, long timestamp)
    {
        return mOscillator.generateComplexSamples(sampleCount, timestamp);
    }

    /**
     * Mixes the complex I & Q samples with samples generated from an oscillator.
     * @param samples to mix
     * @return mixed samples
     */
    public ComplexSamples mix(ComplexSamples samples)
    {
        return mix(samples.i(), samples.q(), samples.timestamp());
    }

    /**
     * Mixes the complex I & Q samples with samples generated from an oscillator
     * @param samples interleaved
     * @return mixed samples
     */
    public ComplexSamples mix(InterleavedComplexSamples samples)
    {
        return mix(samples.toDeinterleaved());
    }

    /**
     * Mixes the complex I & Q samples with samples generated from an oscillator.
     * @param i complex samples to mix
     * @param q complex samples to mix
     * @return mixed samples
     */
    public abstract ComplexSamples mix(float[] i, float[] q, long timestamp);
}
