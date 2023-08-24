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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating DMR Soft Sync Detectors
 */
public class DMRSoftSyncDetectorFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(DMRSoftSyncDetectorFactory.class);

    /**
     * Creates the implementation using calibration data to select the optimal
     * implementation from scalar and vector options.
     * @return demodulator instance
     */
    public static DMRSoftSyncDetector getDetector()
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.DMR_SOFT_SYNC_DETECTOR);

        switch(implementation)
        {
            case VECTOR_SIMD_64:
                return new DMRSoftSyncDetectorVector64();
            case VECTOR_SIMD_128:
                return new DMRSoftSyncDetectorVector128();
            case VECTOR_SIMD_256:
                return new DMRSoftSyncDetectorVector256();
            case VECTOR_SIMD_512:
                return new DMRSoftSyncDetectorVector512();
            case SCALAR:
            default:
                return new DMRSoftSyncDetectorScalar();
        }
    }
}
