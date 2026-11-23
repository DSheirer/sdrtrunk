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

package io.github.dsheirer.module.decode.dmr.sync;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;

/**
 * Factory for creating DMR Soft Sync Detectors
 */
public class DMRSoftSyncDetectorFactory
{

    /**
     * Creates the implementation using calibration data to select the optimal
     * implementation from scalar and vector options.
     * @return demodulator instance
     */
    public static DMRSoftSyncDetector getDetector()
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.DMR_SOFT_SYNC_DETECTOR);
        return getDetector(implementation);
    }

    /**
     * Creates the DMR soft sync detector implementation.
     *
     * Calibration creates its candidates with this method so that it measures exactly what the application uses.
     * @param implementation to create, where UNCALIBRATED creates the scalar implementation
     * @throws IllegalArgumentException if there is no such implementation
     */
    public static DMRSoftSyncDetector getDetector(Implementation implementation)
    {
        return switch(implementation)
        {
            case SCALAR, UNCALIBRATED -> new DMRSoftSyncDetectorScalar();
            case VECTOR_SIMD_64 -> new DMRSoftSyncDetectorVector64();
            case VECTOR_SIMD_128 -> new DMRSoftSyncDetectorVector128();
            case VECTOR_SIMD_256 -> new DMRSoftSyncDetectorVector256();
            case VECTOR_SIMD_512 -> new DMRSoftSyncDetectorVector512();
            default -> throw new IllegalArgumentException("No DMR soft sync detector implementation for " +
                implementation);
        };
    }
}
