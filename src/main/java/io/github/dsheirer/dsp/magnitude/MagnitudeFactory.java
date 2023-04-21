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

package io.github.dsheirer.dsp.magnitude;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating magnitude implementations
 */
public class MagnitudeFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(MagnitudeFactory.class);

    /**
     * Creates a magnitude implementation using calibration data to select the optimal
     * implementation from scalar and vector options.
     * @return demodulator instance
     */
    public static IMagnitudeCalculator getMagnitudeCalculator()
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.MAGNITUDE);

        switch(implementation)
        {
            case VECTOR_SIMD_64:
                return new VectorMagnitudeCalculator64();
            case VECTOR_SIMD_128:
                return new VectorMagnitudeCalculator128();
            case VECTOR_SIMD_256:
                return new VectorMagnitudeCalculator256();
            case VECTOR_SIMD_512:
                return new VectorMagnitudeCalculator512();
            case SCALAR:
            default:
                return new ScalarMagnitudeCalculator();
        }
    }
}
