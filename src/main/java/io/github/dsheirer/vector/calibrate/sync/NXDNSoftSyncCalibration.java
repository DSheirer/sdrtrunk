/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.vector.calibrate.sync;

import io.github.dsheirer.module.decode.nxdn.layer1.sync.NXDNSoftSyncDetector;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.NXDNSyncDetectorFactory;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import java.util.List;

/**
 * NXDN soft sync detector calibration
 */
public class NXDNSoftSyncCalibration extends Calibration
{
    private static final int BUFFER_SIZE = 2048;
    private final float[] mSymbols;

    /**
     * Constructs an instance
     */
    public NXDNSoftSyncCalibration()
    {
        super(CalibrationType.NXDN_SOFT_SYNC_DETECTOR);
        mSymbols = getFloatSamples(BUFFER_SIZE);
    }

    @Override protected List<Candidate> getCandidates()
    {
        return List.of(
            candidate(Implementation.SCALAR, () -> {
                NXDNSoftSyncDetector detector = NXDNSyncDetectorFactory.getStandardDetector(Implementation.SCALAR);
                float[] scores = new float[mSymbols.length];
                return () -> {
                    for(int x = 0; x < mSymbols.length; x++)
                    {
                        scores[x] = detector.process(mSymbols[x]);
                    }
                    return scores;
                };
            }),
            candidate(Implementation.VECTOR_SIMD_64, () -> {
                NXDNSoftSyncDetector detector = NXDNSyncDetectorFactory.getStandardDetector(
                    Implementation.VECTOR_SIMD_64);
                float[] scores = new float[mSymbols.length];
                return () -> {
                    for(int x = 0; x < mSymbols.length; x++)
                    {
                        scores[x] = detector.process(mSymbols[x]);
                    }
                    return scores;
                };
            }),
            candidate(Implementation.VECTOR_SIMD_128, () -> {
                NXDNSoftSyncDetector detector = NXDNSyncDetectorFactory.getStandardDetector(
                    Implementation.VECTOR_SIMD_128);
                float[] scores = new float[mSymbols.length];
                return () -> {
                    for(int x = 0; x < mSymbols.length; x++)
                    {
                        scores[x] = detector.process(mSymbols[x]);
                    }
                    return scores;
                };
            }),
            candidate(Implementation.VECTOR_SIMD_256, () -> {
                NXDNSoftSyncDetector detector = NXDNSyncDetectorFactory.getStandardDetector(
                    Implementation.VECTOR_SIMD_256);
                float[] scores = new float[mSymbols.length];
                return () -> {
                    for(int x = 0; x < mSymbols.length; x++)
                    {
                        scores[x] = detector.process(mSymbols[x]);
                    }
                    return scores;
                };
            }),
            candidate(Implementation.VECTOR_SIMD_512, () -> {
                NXDNSoftSyncDetector detector = NXDNSyncDetectorFactory.getStandardDetector(
                    Implementation.VECTOR_SIMD_512);
                float[] scores = new float[mSymbols.length];
                return () -> {
                    for(int x = 0; x < mSymbols.length; x++)
                    {
                        scores[x] = detector.process(mSymbols[x]);
                    }
                    return scores;
                };
            }));
    }
}
