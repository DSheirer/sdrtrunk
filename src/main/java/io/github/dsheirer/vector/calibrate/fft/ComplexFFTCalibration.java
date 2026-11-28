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
package io.github.dsheirer.vector.calibrate.fft;

import io.github.dsheirer.dsp.fft.ComplexFFT;
import io.github.dsheirer.dsp.fft.ComplexFFTFactory;
import io.github.dsheirer.dsp.fft.VectorComplexFFT;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import java.util.List;

/**
 * Complex FFT calibration: a batch of small inverse transforms, as the polyphase channelizer uses, and a single larger
 * forward transform, as the spectrum display uses.
 */
public class ComplexFFTCalibration extends Calibration
{
    private static final int BATCH_SIZE = 96;
    private static final int BATCH_COUNT = 64;
    private static final int SINGLE_SIZE = 2048;
    private static final double TOLERANCE = 1e-4;
    private final float[] mBatchSamples;
    private final float[] mSingleSamples;

    /**
     * Constructs an instance
     */
    public ComplexFFTCalibration()
    {
        super(CalibrationType.FFT_COMPLEX);
        mBatchSamples = getFloatSamples(BATCH_SIZE * BATCH_COUNT * 2);
        mSingleSamples = getFloatSamples(SINGLE_SIZE * 2);
    }

    /**
     * The scalar (mixed radix) and vector FFTs use different algorithms, which round differently.
     */
    @Override protected double getTolerance()
    {
        return TOLERANCE;
    }

    @Override protected List<Candidate> getCandidates()
    {
        if(!VectorComplexFFT.isVectorSupported())
        {
            return List.of(candidate(Implementation.SCALAR));
        }

        return List.of(candidate(Implementation.SCALAR), candidate(Implementation.VECTOR_SIMD_PREFERRED));
    }

    private Candidate candidate(Implementation implementation)
    {
        return candidate(implementation, () -> {
            ComplexFFT batch = ComplexFFTFactory.getComplexFFT(implementation, BATCH_SIZE);
            ComplexFFT single = ComplexFFTFactory.getComplexFFT(implementation, SINGLE_SIZE,
                ComplexFFTFactory.Scalar.JTRANSFORMS);
            return () -> {
                //The FFTs are processed in place
                float[] batchSamples = mBatchSamples.clone();
                float[] singleSamples = mSingleSamples.clone();
                batch.inverse(batchSamples, 0, BATCH_COUNT, true);
                single.forward(singleSamples);
                return new float[][]{batchSamples, singleSamples};
            };
        });
    }
}
