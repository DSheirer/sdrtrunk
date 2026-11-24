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

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;

/**
 * Factory for creating complex mixer instances from scalar and vector implementations.
 */
public class ComplexMixerFactory
{
    /**
     * Creates an instance of the optimal implementation of a complex mixer for this hardware.
     * @param frequency of the mixing oscillator
     * @param sampleRate of the mixing oscillator
     * @return optimal instance
     */
    public static ComplexMixer getMixer(double frequency, double sampleRate)
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.MIXER_COMPLEX);
        return getMixer(implementation, frequency, sampleRate);
    }

    /**
     * Creates the complex mixer implementation.
     *
     * Calibration creates its candidates with this method so that it measures exactly what the application uses.
     * @param implementation to create, where UNCALIBRATED creates the scalar implementation
     * @throws IllegalArgumentException if there is no such implementation
     */
    public static ComplexMixer getMixer(Implementation implementation, double frequency, double sampleRate)
    {
        return switch(implementation)
        {
            case SCALAR, UNCALIBRATED -> new ScalarComplexMixer(frequency, sampleRate);
            case VECTOR_SIMD_PREFERRED -> new VectorComplexMixer(frequency, sampleRate);
            default -> throw new IllegalArgumentException("No complex mixer implementation for " +
                implementation);
        };
    }
}
