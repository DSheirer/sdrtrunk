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

package io.github.dsheirer.dsp.mixer;

import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;

/**
 * Scalar implementation of a complex mixer
 */
public class ScalarComplexMixer extends ComplexMixer
{
    /**
     * Constructs an instance
     * @param frequency of the mixing oscillator
     * @param sampleRate of the mixing oscillator
     * @param disableSIMD to disable use of SIMD vector instructions
     */
    public ScalarComplexMixer(double frequency, double sampleRate, boolean disableSIMD)
    {
        super(frequency, sampleRate, disableSIMD);
    }

    /**
     * Constructs an instance
     * @param frequency of the mixing oscillator
     * @param sampleRate of the mixing oscillator
     */
    public ScalarComplexMixer(double frequency, double sampleRate)
    {
        super(frequency, sampleRate);
    }

    /**
     * Mixes the complex I & Q samples with samples generated from an oscillator.
     * @param iSamples complex samples to mix
     * @param qSamples complex samples to mix
     * @param timestamp of the first sample
     * @return mixed samples
     */
    @Override public ComplexSamples mix(float[] iSamples, float[] qSamples, long timestamp)
    {
        ComplexSamples mixer = generate(iSamples.length, timestamp);

        float[] iMixer = mixer.i();
        float[] qMixer = mixer.q();

        float inphase, quadrature;

        for(int x = 0; x < iSamples.length; x++)
        {
            inphase = (iMixer[x] * iSamples[x]) - (qMixer[x] * qSamples[x]);
            quadrature = (qMixer[x] * iSamples[x]) + (iMixer[x] * qSamples[x]);
            iMixer[x] = inphase;
            qMixer[x] = quadrature;
        }

        return mixer;
    }

    /**
     * Mixes the interleaved complex samples with oscillator generated samples
     * @param samples interleaved
     * @return mixed samples
     */
    @Override
    public ComplexSamples mix(InterleavedComplexSamples samples)
    {
        ComplexSamples mixer = generate(samples.samples().length / 2, samples.timestamp());

        float[] iMixer = mixer.i();
        float[] qMixer = mixer.q();

        float inphase, quadrature;
        int offset = 0;

        for(int x = 0; x < iMixer.length; x++)
        {
            offset = 2 * x;
            inphase = (iMixer[x] * samples.samples()[offset]) - (qMixer[x] * samples.samples()[offset + 1]);
            quadrature = (qMixer[x] * samples.samples()[offset]) + (iMixer[x] * samples.samples()[offset + 1]);
            iMixer[x] = inphase;
            qMixer[x] = quadrature;
        }

        return mixer;
    }
}
