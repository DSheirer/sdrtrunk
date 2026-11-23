/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.vector.calibrate;

import jdk.incubator.vector.FloatVector;

/**
 * Identifies the optimal implementation (Scalar vs Vector) identified through calibration testing
 */
public enum Implementation
{
    SCALAR(0),
    VECTOR_SIMD_PREFERRED(0),
    VECTOR_SIMD_64(64),
    VECTOR_SIMD_128(128),
    VECTOR_SIMD_256(256),
    VECTOR_SIMD_512(512),
    UNCALIBRATED(0);

    private final int mVectorBitSize;

    Implementation(int vectorBitSize)
    {
        mVectorBitSize = vectorBitSize;
    }

    /**
     * Fixed vector width of this implementation, or zero for scalar and the CPU's preferred (native) width.
     */
    public int getVectorBitSize()
    {
        return mVectorBitSize;
    }

    /**
     * Vector width that this implementation uses on this CPU: the fixed width, the CPU's preferred width for
     * VECTOR_SIMD_PREFERRED, or zero for scalar and uncalibrated.
     */
    public int getEffectiveVectorBitSize()
    {
        if(this == VECTOR_SIMD_PREFERRED)
        {
            return FloatVector.SPECIES_PREFERRED.vectorBitSize();
        }

        return mVectorBitSize;
    }

    /**
     * Indicates if the CPU supports this implementation natively.  The Vector API emulates vector widths that are
     * wider than the CPU's preferred width, which is correct but much slower than scalar.
     */
    public boolean isNativelySupported()
    {
        return mVectorBitSize <= FloatVector.SPECIES_PREFERRED.vectorBitSize();
    }
}
