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

package io.github.dsheirer.dsp.gain.complex;

import io.github.dsheirer.sample.complex.ComplexSamples;

/**
 * Scalar implementation of complex gain
 */
public class ScalarComplexGain extends ComplexGain
{
    /**
     * Constructs an instance
     * @param gain to apply to complex samples
     */
    public ScalarComplexGain(float gain)
    {
        super(gain);
    }

    /**
     * Applies gain to the complex samples buffer
     * @param i samples to amplify
     * @param q samples to amplify
     * @return amplified samples
     */
    @Override public ComplexSamples apply(float[] i, float[] q, long timestamp)
    {
        for(int x = 0; x < i.length; x++)
        {
            i[x] *= mGain;
            q[x] *= mGain;
        }

        return new ComplexSamples(i, q, timestamp);
    }
}
