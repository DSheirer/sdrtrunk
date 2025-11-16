/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.dsp.psk.demod;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;

/**
 * Factory for creating PSK differential demodulators
 */
public class DifferentialDemodulatorFactory
{
    /**
     * Creates the optimal float output demodulator using calibration data to select the optimal implementation from
     * scalar and vector options, or defaults to the scalar implementation if the calibration wasn't executed.
     * @param sampleRate in Hertz
     * @param symbolRate in Hertz
     * @return demodulator instance
     */
    public static DifferentialDemodulatorFloat getFloatDemodulator(double sampleRate, int symbolRate)
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.DIFFERENTIAL_DEMODULATOR);

        return switch(implementation)
        {
            case VECTOR_SIMD_64 -> new DifferentialDemodulatorFloatVector64(sampleRate, symbolRate);
            case VECTOR_SIMD_128 -> new DifferentialDemodulatorFloatVector128(sampleRate, symbolRate);
            case VECTOR_SIMD_256 -> new DifferentialDemodulatorFloatVector256(sampleRate, symbolRate);
            case VECTOR_SIMD_512 -> new DifferentialDemodulatorFloatVector512(sampleRate, symbolRate);
            default -> new DifferentialDemodulatorFloatScalar(sampleRate, symbolRate); //Includes SCALAR case
        };
    }
}
