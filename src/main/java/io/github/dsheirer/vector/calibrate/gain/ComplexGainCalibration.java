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

package io.github.dsheirer.vector.calibrate.gain;

import io.github.dsheirer.dsp.gain.complex.ComplexGain;
import io.github.dsheirer.dsp.gain.complex.ComplexGainFactory;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import java.util.List;

/**
 * Complex gain calibration
 */
public class ComplexGainCalibration extends Calibration
{
    private static final int BUFFER_SIZE = 2048;
    private static final float GAIN = 0.99f;
    private final float[] mI;
    private final float[] mQ;

    /**
     * Constructs an instance
     */
    public ComplexGainCalibration()
    {
        super(CalibrationType.GAIN_COMPLEX);
        mI = getFloatSamples(BUFFER_SIZE);
        mQ = getFloatSamples(BUFFER_SIZE);
    }

    @Override protected List<Candidate> getCandidates()
    {
        return List.of(
            candidate(Implementation.SCALAR, () -> {
                ComplexGain operation = ComplexGainFactory.getComplexGain(Implementation.SCALAR, GAIN);
                return () -> {
                    //The gain is applied in place
                    return operation.apply(mI.clone(), mQ.clone(), 0L);
                };
            }),
            candidate(Implementation.VECTOR_SIMD_PREFERRED, () -> {
                ComplexGain operation = ComplexGainFactory.getComplexGain(Implementation.VECTOR_SIMD_PREFERRED, GAIN);
                return () -> {
                    //The gain is applied in place
                    return operation.apply(mI.clone(), mQ.clone(), 0L);
                };
            }));
    }
}
