/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.dsp.psk.dqpsk;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating DQPSK demodulators
 */
public class DQPSKDemodulatorFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(DQPSKDemodulatorFactory.class);

    /**
     * Creates the optimal demodulator using calibration data to select the optimal implementation from scalar and
     * vector options, or defaults to the scalar implementation if the calibration wasn't executed.
     * @param sampleRate in Hertz
     * @param symbolRate in Hertz
     * @return demodulator instance
     */
    public static DQPSKDemodulator getDemodulator(double sampleRate, int symbolRate)
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.DQPSK_DEMODULATOR);

        return switch(implementation)
        {
            case VECTOR_SIMD_64 -> new DQPSKDemodulatorVector64(sampleRate, symbolRate);
            case VECTOR_SIMD_128 -> new DQPSKDemodulatorVector128(sampleRate, symbolRate);
            case VECTOR_SIMD_256 -> new DQPSKDemodulatorVector256(sampleRate, symbolRate);
            case VECTOR_SIMD_512 -> new DQPSKDemodulatorVector512(sampleRate, symbolRate);
            default -> new DQPSKDemodulatorScalar(sampleRate, symbolRate); //Includes SCALAR case
        };
    }
}
