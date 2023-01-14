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

import io.github.dsheirer.sample.buffer.ITimestamped;

/**
 * Complex samples array that incorporates a timestamp for the first complex sample.
 */
public record InterleavedComplexSamples(float[] samples, long timestamp) implements ITimestamped
{
    /**
     * Converts this interleaved complex samples to de-interleaved.
     * @return de-interleaved samples.
     */
    public ComplexSamples toDeinterleaved()
    {
        float[] i = new float[samples().length / 2];
        float[] q = new float[samples().length / 2];

        for(int x = 0; x < i.length; x++)
        {
            i[x] = samples()[x / 2];
            q[x] = samples()[x / 2 + 1];
        }

        return new ComplexSamples(i, q, timestamp());
    }
}
