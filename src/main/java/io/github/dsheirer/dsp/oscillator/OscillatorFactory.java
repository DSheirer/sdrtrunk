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

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;

/**
 * Factory class for creating real and complex oscillators.
 *
 * Uses the CalibrationManager to determine the optimal (scalar vs vector) oscillator implementation type.
 */
public class OscillatorFactory
{

    /**
     * Constructs an optimal implementation of a real sample oscillator using calibration data when available.
     * @param frequency of the oscillator
     * @param sampleRate of the oscillator
     * @return constructed oscillator
     */
    public static IRealOscillator getRealOscillator(double frequency, double sampleRate)
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.OSCILLATOR_REAL);
        return getRealOscillator(implementation, frequency, sampleRate);
    }

    /**
     * Creates the real oscillator implementation.
     *
     * Calibration creates its candidates with this method so that it measures exactly what the application uses.
     * @param implementation to create, where UNCALIBRATED creates the scalar implementation
     * @throws IllegalArgumentException if there is no such implementation
     */
    public static IRealOscillator getRealOscillator(Implementation implementation, double frequency, double sampleRate)
    {
        return switch(implementation)
        {
            case SCALAR, UNCALIBRATED -> new ScalarRealOscillator(frequency, sampleRate);
            case VECTOR_SIMD_PREFERRED -> new VectorRealOscillator(frequency, sampleRate);
            default -> throw new IllegalArgumentException("No real oscillator implementation for " +
                implementation);
        };
    }

    /**
     * Constructs an optimal implementation of a complex sample oscillator using calibration data when available.
     * @param frequency of the oscillator
     * @param sampleRate of the oscillator
     * @return constructed oscillator
     */
    public static IComplexOscillator getComplexOscillator(double frequency, double sampleRate)
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.OSCILLATOR_COMPLEX);
        return getComplexOscillator(implementation, frequency, sampleRate);
    }

    /**
     * Creates the complex oscillator implementation.
     *
     * Calibration creates its candidates with this method so that it measures exactly what the application uses.
     * @param implementation to create, where UNCALIBRATED creates the scalar implementation
     * @throws IllegalArgumentException if there is no such implementation
     */
    public static IComplexOscillator getComplexOscillator(Implementation implementation, double frequency,
                                                          double sampleRate)
    {
        return switch(implementation)
        {
            case SCALAR, UNCALIBRATED -> new ScalarComplexOscillator(frequency, sampleRate);
            case VECTOR_SIMD_PREFERRED -> new VectorComplexOscillator(frequency, sampleRate);
            default -> throw new IllegalArgumentException("No complex oscillator implementation for " +
                implementation);
        };
    }
}
