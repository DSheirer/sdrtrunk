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

package io.github.dsheirer.module.decode.nxdn.layer1.sync;

import io.github.dsheirer.module.decode.nxdn.layer1.sync.control.NXDNControlSoftSyncDetector;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.control.NXDNControlSoftSyncDetectorScalar;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.control.NXDNControlSoftSyncDetectorVector128;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.control.NXDNControlSoftSyncDetectorVector256;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.control.NXDNControlSoftSyncDetectorVector512;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.control.NXDNControlSoftSyncDetectorVector64;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.standard.NXDNStandardSoftSyncDetector;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.standard.NXDNStandardSoftSyncDetectorScalar;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.standard.NXDNStandardSoftSyncDetectorVector128;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.standard.NXDNStandardSoftSyncDetectorVector256;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.standard.NXDNStandardSoftSyncDetectorVector512;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.standard.NXDNStandardSoftSyncDetectorVector64;
import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating NXDN Soft Sync Detectors
 */
public class NXDNSyncDetectorFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(NXDNSyncDetectorFactory.class);

    /**
     * Creates the implementation using calibration data to select the optimal implementation from scalar and vector options.
     * @return demodulator instance
     */
    public static NXDNStandardSoftSyncDetector getStandardDetector()
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.NXDN_SOFT_SYNC_DETECTOR);

        switch(implementation)
        {
            case VECTOR_SIMD_64:
                return new NXDNStandardSoftSyncDetectorVector64();
            case VECTOR_SIMD_128:
                return new NXDNStandardSoftSyncDetectorVector128();
            case VECTOR_SIMD_256:
                return new NXDNStandardSoftSyncDetectorVector256();
            case VECTOR_SIMD_512:
                return new NXDNStandardSoftSyncDetectorVector512();
            case SCALAR:
            default:
                return new NXDNStandardSoftSyncDetectorScalar();
        }
    }

    /**
     * Creates the implementation using calibration data to select the optimal implementation from scalar and vector options.
     * @return demodulator instance
     */
    public static NXDNControlSoftSyncDetector getControlDetector()
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.NXDN_SOFT_SYNC_DETECTOR);

        switch(implementation)
        {
            case VECTOR_SIMD_64:
                return new NXDNControlSoftSyncDetectorVector64();
            case VECTOR_SIMD_128:
                return new NXDNControlSoftSyncDetectorVector128();
            case VECTOR_SIMD_256:
                return new NXDNControlSoftSyncDetectorVector256();
            case VECTOR_SIMD_512:
                return new NXDNControlSoftSyncDetectorVector512();
            case SCALAR:
            default:
                return new NXDNControlSoftSyncDetectorScalar();
        }
    }
}
