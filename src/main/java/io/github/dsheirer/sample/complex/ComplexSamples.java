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

package io.github.dsheirer.sample.complex;

/**
 * Wrapper for a complex sample array where I and Q samples are in separate arrays.
 */
public record ComplexSamples(float[] i, float[] q, long timestamp)
{
    /**
     * Converts this non-interleaved complex samples to interleaved.
     * @return interleaved samples.
     */
    public InterleavedComplexSamples toInterleaved()
    {
        float[] interleaved = new float[i().length * 2];

        for(int x = 0; x < i().length; x++)
        {
            interleaved[2 * x] = i()[x];
            interleaved[2 * x + 1] = q()[x];
        }

        return new InterleavedComplexSamples(interleaved, System.currentTimeMillis());
    }
}

