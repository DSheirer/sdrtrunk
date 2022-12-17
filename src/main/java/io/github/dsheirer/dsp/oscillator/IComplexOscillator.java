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

package io.github.dsheirer.dsp.oscillator;

import io.github.dsheirer.sample.complex.ComplexSamples;

public interface IComplexOscillator extends IOscillator
{
    /**
     * Generates the specified number of complex samples into an interleaved sample array.
     * @param sampleCount number of complex samples to generate.
     * @return generated samples where each sample is interleaved: I0, Q0, I1, Q1 ...
     */
    float[] generate(int sampleCount);

    /**
     * Generates the specified number of complex samples into a complex samples object.
     * @param sampleCount number of complex samples to generate.
     * @param timestamp for the first sample
     * @return generated samples
     */
    ComplexSamples generateComplexSamples(int sampleCount, long timestamp);
}
