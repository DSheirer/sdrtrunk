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

package io.github.dsheirer.module.decode.nxdn.sync;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating NXDN Soft Sync Detectors
 */
public class NXDNSoftSyncDetectorFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(NXDNSoftSyncDetectorFactory.class);

    /**
     * Creates the implementation using calibration data to select the optimal implementation from scalar and vector options.
     * @return demodulator instance
     */
    public static NXDNSoftSyncDetector getDetector()
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.P25P1_SOFT_SYNC_DETECTOR);

        switch(implementation)
        {
            case VECTOR_SIMD_64:
                return new NXDNSoftSyncDetectorVector64();
            case VECTOR_SIMD_128:
                return new NXDNSoftSyncDetectorVector128();
            case VECTOR_SIMD_256:
                return new NXDNSoftSyncDetectorVector256();
            case VECTOR_SIMD_512:
                return new NXDNSoftSyncDetectorVector512();
            case SCALAR:
            default:
                return new NXDNSoftSyncDetectorScalar();
        }
    }
}
