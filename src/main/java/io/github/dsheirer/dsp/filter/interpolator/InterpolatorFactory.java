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

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;

/**
 * Factory for creating an optimal scalar or vector (SIMD) interpolator implementation
 */
public class InterpolatorFactory
{
    /**
     * Selects and instantiates the best interpolator version, scalar or vector, based on previous calibration.
     * @return interpolator
     */
    public static Interpolator getInterpolator()
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.INTERPOLATOR);
        return getInterpolator(implementation);
    }

    /**
     * Instantiates the specified interpolator implementation.
     * @param implementation to construct.
     * @return interpolator.
     */
    public static Interpolator getInterpolator(Implementation implementation)
    {
        return switch(implementation)
        {
            case SCALAR, UNCALIBRATED -> new RealInterpolator();
            case VECTOR_SIMD_PREFERRED -> new VectorInterpolotorPreferred();
            case VECTOR_SIMD_64 -> new VectorInterpolator64();
            case VECTOR_SIMD_128 -> new VectorInterpolator128();
            case VECTOR_SIMD_256, VECTOR_SIMD_512 -> new VectorInterpolator256(); //Vector 512 not supported
        };
    }
}
