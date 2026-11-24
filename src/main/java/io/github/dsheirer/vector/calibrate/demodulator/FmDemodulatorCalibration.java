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

package io.github.dsheirer.vector.calibrate.demodulator;

import io.github.dsheirer.dsp.fm.FmDemodulatorFactory;
import io.github.dsheirer.dsp.fm.IDemodulator;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import java.util.List;

/**
 * FM demodulator calibration
 */
public class FmDemodulatorCalibration extends Calibration
{
    private static final int BUFFER_SIZE = 2048;
    private final float[] mI;
    private final float[] mQ;

    /**
     * Constructs an instance
     */
    public FmDemodulatorCalibration()
    {
        super(CalibrationType.FM_DEMODULATOR);
        mI = getFloatSamples(BUFFER_SIZE);
        mQ = getFloatSamples(BUFFER_SIZE);
    }

    @Override protected List<Candidate> getCandidates()
    {
        return List.of(
            candidate(Implementation.SCALAR, () -> {
                IDemodulator operation = FmDemodulatorFactory.getFmDemodulator(Implementation.SCALAR);
                return () -> operation.demodulate(mI, mQ);
            }),
            candidate(Implementation.VECTOR_SIMD_64, () -> {
                IDemodulator operation = FmDemodulatorFactory.getFmDemodulator(Implementation.VECTOR_SIMD_64);
                return () -> operation.demodulate(mI, mQ);
            }),
            candidate(Implementation.VECTOR_SIMD_128, () -> {
                IDemodulator operation = FmDemodulatorFactory.getFmDemodulator(Implementation.VECTOR_SIMD_128);
                return () -> operation.demodulate(mI, mQ);
            }),
            candidate(Implementation.VECTOR_SIMD_256, () -> {
                IDemodulator operation = FmDemodulatorFactory.getFmDemodulator(Implementation.VECTOR_SIMD_256);
                return () -> operation.demodulate(mI, mQ);
            }),
            candidate(Implementation.VECTOR_SIMD_512, () -> {
                IDemodulator operation = FmDemodulatorFactory.getFmDemodulator(Implementation.VECTOR_SIMD_512);
                return () -> operation.demodulate(mI, mQ);
            }));
    }
}
