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

package io.github.dsheirer.dsp.filter.interpolator;

import org.apache.commons.lang3.Validate;

/**
 * Base vector interpolator implementation.
 */
public abstract class VectorInterpolator extends Interpolator
{
    @Override
    public float filter(float[] samples, int offset, float mu)
    {
        /* Ensure we have enough samples in the array */
        Validate.isTrue(samples.length >= offset + 7, "Offset [" + offset + "] must be 7 less than length[" + samples.length + "]");

        //Identify the filter bank that corresponds to mu.  Note: since we're not loading the TAPS in reverse order,
        //we select the inverse tap index that has the mirrored set of taps, by subtracting from 127.
        int index = 127 - (int)(NSTEPS * mu);
        return vectorFilter(samples, offset, index);
    }

    /**
     * Perform vectorized interpolation to find the interpolated value from the samples where the 8-value sequence
     * starts at the offset into the array, using the filter coefficients specified by the index value.
     * @param samples to interpolate
     * @param offset to the start of the 8-value sequence
     * @param index into the TAPS coefficients array.
     * @return interpolated value.
     */
    protected abstract float vectorFilter(float[] samples, int offset, int index);
}
