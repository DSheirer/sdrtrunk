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

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;

/**
 * Factory for selecting and creating the optimal implementation of complex gain for this hardware.
 */
public class ComplexGainFactory
{
    /**
     * Instantiates the optimal complex gain implementation for this hardware,
     * as determined by the Calibration Manager.
     */
    public static ComplexGain getComplexGain(float gain)
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.GAIN_COMPLEX);
        return getComplexGain(implementation, gain);
    }

    /**
     * Creates the complex gain implementation.
     *
     * Calibration creates its candidates with this method so that it measures exactly what the application uses.
     * @param implementation to create, where UNCALIBRATED creates the scalar implementation
     * @throws IllegalArgumentException if there is no such implementation
     */
    public static ComplexGain getComplexGain(Implementation implementation, float gain)
    {
        return switch(implementation)
        {
            case SCALAR, UNCALIBRATED -> new ScalarComplexGain(gain);
            case VECTOR_SIMD_PREFERRED -> new VectorComplexGain(gain);
            default -> throw new IllegalArgumentException("No complex gain implementation for " +
                implementation);
        };
    }
}
