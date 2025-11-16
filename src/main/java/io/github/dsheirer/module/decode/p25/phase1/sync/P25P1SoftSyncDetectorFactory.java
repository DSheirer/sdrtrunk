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

package io.github.dsheirer.module.decode.p25.phase1.sync;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating P25 Phase 1 Soft Sync Detectors
 */
public class P25P1SoftSyncDetectorFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(P25P1SoftSyncDetectorFactory.class);

    /**
     * Creates the implementation using calibration data to select the optimal
     * implementation from scalar and vector options.
     * @return demodulator instance
     */
    public static P25P1SoftSyncDetector getDetector()
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.P25P1_SOFT_SYNC_DETECTOR);

        switch(implementation)
        {
            case VECTOR_SIMD_64:
                return new P25P1SoftSyncDetectorVector64();
            case VECTOR_SIMD_128:
                return new P25P1SoftSyncDetectorVector128();
            case VECTOR_SIMD_256:
                return new P25P1SoftSyncDetectorVector256();
            case VECTOR_SIMD_512:
                return new P25P1SoftSyncDetectorVector512();
            case SCALAR:
            default:
                return new P25P1SoftSyncDetectorScalar();
        }
    }
}
