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

package io.github.dsheirer.vector.calibrate.filter;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import java.util.List;

/**
 * Real FIR filter calibration
 */
public class FirFilterCalibration extends Calibration
{
    private static final int BUFFER_SIZE = 2048;
    private final float[] mCoefficients;
    private final float[] mSamples;

    /**
     * Constructs an instance
     */
    public FirFilterCalibration()
    {
        super(CalibrationType.FILTER_FIR);
        mCoefficients = FilterFactory.getSinc(0.25, 31, WindowType.BLACKMAN);
        mSamples = getFloatSamples(BUFFER_SIZE);
    }

    @Override protected List<Candidate> getCandidates()
    {
        return List.of(
            candidate(Implementation.SCALAR, () -> {
                IRealFilter operation = FilterFactory.getRealFilter(Implementation.SCALAR, mCoefficients.clone());
                return () -> operation.filter(mSamples);
            }),
            candidate(Implementation.VECTOR_SIMD_PREFERRED, () -> {
                IRealFilter operation = FilterFactory.getRealFilter(
                    Implementation.VECTOR_SIMD_PREFERRED, mCoefficients.clone());
                return () -> operation.filter(mSamples);
            }),
            candidate(Implementation.VECTOR_SIMD_64, () -> {
                IRealFilter operation = FilterFactory.getRealFilter(
                    Implementation.VECTOR_SIMD_64, mCoefficients.clone());
                return () -> operation.filter(mSamples);
            }),
            candidate(Implementation.VECTOR_SIMD_128, () -> {
                IRealFilter operation = FilterFactory.getRealFilter(
                    Implementation.VECTOR_SIMD_128, mCoefficients.clone());
                return () -> operation.filter(mSamples);
            }),
            candidate(Implementation.VECTOR_SIMD_256, () -> {
                IRealFilter operation = FilterFactory.getRealFilter(
                    Implementation.VECTOR_SIMD_256, mCoefficients.clone());
                return () -> operation.filter(mSamples);
            }),
            candidate(Implementation.VECTOR_SIMD_512, () -> {
                IRealFilter operation = FilterFactory.getRealFilter(
                    Implementation.VECTOR_SIMD_512, mCoefficients.clone());
                return () -> operation.filter(mSamples);
            }));
    }

    /**
     * Vector filters pad the coefficients to a multiple of the lane count, which delays their output by the padding.
     */
    @Override protected int getMaximumDelay()
    {
        return 16;
    }
}
