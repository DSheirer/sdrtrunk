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
package io.github.dsheirer.dsp.fft;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;

/**
 * Factory for selecting the optimal implementation of the complex FFT for this hardware, as determined by the
 * Calibration Manager.
 */
public class ComplexFFTFactory
{
    /**
     * Scalar FFT to use where calibration selects scalar, so that each user keeps the FFT it used before the vector
     * FFT.
     */
    public enum Scalar
    {
        /**
         * The mixed radix FFT at every size.
         */
        MIXED_RADIX,
        /**
         * JTransforms at power of two sizes, including its thread pool, and the mixed radix FFT at any other size.
         */
        JTRANSFORMS
    }

    /**
     * Creates the optimal complex FFT implementation, with the mixed radix FFT where calibration selects scalar.
     * @param size of the transform in complex samples, with no prime factors other than 2, 3 and 5
     * @return complex FFT
     * @throws IllegalArgumentException if the size is not supported
     */
    public static ComplexFFT getComplexFFT(int size)
    {
        return getComplexFFT(size, Scalar.MIXED_RADIX);
    }

    /**
     * Creates the optimal complex FFT implementation.
     * @param size of the transform in complex samples, with no prime factors other than 2, 3 and 5
     * @param scalar FFT to use where calibration selects scalar
     * @return complex FFT
     * @throws IllegalArgumentException if the size is not supported
     */
    public static ComplexFFT getComplexFFT(int size, Scalar scalar)
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.FFT_COMPLEX);
        return getComplexFFT(implementation, size, scalar);
    }

    /**
     * Creates the complex FFT implementation, with the mixed radix FFT for the scalar implementation.
     * @param implementation to create (see getComplexFFT(Implementation, int, Scalar))
     * @param size of the transform in complex samples, with no prime factors other than 2, 3 and 5
     * @throws IllegalArgumentException if the size is not supported
     */
    public static ComplexFFT getComplexFFT(Implementation implementation, int size)
    {
        return getComplexFFT(implementation, size, Scalar.MIXED_RADIX);
    }

    /**
     * Creates the complex FFT implementation.
     *
     * Calibration creates its candidates with this method so that it measures exactly what the application uses.
     * @param implementation to create.  VECTOR_SIMD_PREFERRED creates the vector FFT on a CPU that supports it (see
     * VectorComplexFFT.isVectorSupported()).  Any other implementation, including UNCALIBRATED, creates the scalar FFT.
     * @param size of the transform in complex samples, with no prime factors other than 2, 3 and 5
     * @param scalar FFT to use for the scalar implementation
     * @throws IllegalArgumentException if the size is not supported
     */
    public static ComplexFFT getComplexFFT(Implementation implementation, int size, Scalar scalar)
    {
        if(implementation == Implementation.VECTOR_SIMD_PREFERRED && VectorComplexFFT.isVectorSupported())
        {
            return new VectorComplexFFT(size);
        }

        return getScalarFFT(size, scalar);
    }

    /**
     * Creates the scalar FFT.
     */
    private static ComplexFFT getScalarFFT(int size, Scalar scalar)
    {
        if(scalar == Scalar.JTRANSFORMS && size > 1 && Integer.bitCount(size) == 1)
        {
            return new JTransformsComplexFFT(size);
        }

        return new MixedRadixComplexFFT(size);
    }
}
