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

        return new InterleavedComplexSamples(interleaved, timestamp());
    }

    /**
     * Length of complex samples available in this buffer.
     * @return sample count.
     */
    public int length()
    {
        return i().length;
    }

    /**
     * Extracts an interleaved buffer of specified count of complex samples starting at the specified offset
     * @param offset into the I or Q sample array
     * @param length of complex samples.
     * @return interleaved complex samples with an array length that is twice as long as the requested length.
     * @throws ArrayIndexOutOfBoundsException if the requested offset + length exceeds the sample count.
     */
    public InterleavedComplexSamples toInterleaved(int offset, int length)
    {
        if(offset + length > i().length)
        {
            throw new ArrayIndexOutOfBoundsException("Offset [" + offset + "] length [" + length +
                    "] out of bounds for length [" + i().length + "]");
        }

        float[] interleaved = new float[length * 2];

        for(int x = 0; x < length; x++)
        {
            interleaved[2 * x] = i()[offset + x];
            interleaved[2 * x + 1] = q()[offset + x];
        }

        return new InterleavedComplexSamples(interleaved, timestamp());
    }
}

