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
import org.jtransforms.fft.FloatFFT_1D;

/**
 * Allocation-free forward FFT of real samples, for even sizes whose half has no prime factors other than 2, 3 and 5.
 * With the scalar implementation a power of two size is the JTransforms real transform, which was the transform in
 * use before this class, including its thread pool for sizes of 8192 and up.
 *
 * The N real samples are transformed as N/2 complex samples z[n] = x[2n] + j x[2n + 1] with a ComplexFFT, and the
 * spectrum of the even and odd samples is then separated and combined:
 * X[k] = (Z[k] + conj(Z[N/2 - k])) / 2 + exp(-j2pi k/N) (Z[k] - conj(Z[N/2 - k])) / 2j.
 *
 * The output layout matches JTransforms FloatFFT_1D realForward() for even sizes: a[0] = Re[0], a[1] = Re[N/2], and
 * a[2k] = Re[k], a[2k + 1] = Im[k] for 0 < k < N/2.  Bins above N/2 are the complex conjugates of the bins below it
 * and are not stored.
 *
 * The samples can also be split: even[n] = x[2n] and odd[n] = x[2n + 1], with the output in the same layout,
 * even[k] = Re[k] and odd[k] = Im[k], with even[0] = Re[0] and odd[0] = Re[N/2].
 *
 * Note: this class is NOT thread-safe since it reuses internal buffers.  Use one instance per thread.
 */
public class RealFFT
{
    private final int mSize;
    private final ComplexFFT mFFT;
    private final float[] mCos;
    private final float[] mSin;
    private final float[] mEven;
    private final float[] mOdd;
    private final FloatFFT_1D mJTransforms;
    /**
     * True when the complex FFT is the vector four-step FFT, which also uses the vector data moves and split step.
     * Below that size the complex FFT is the scalar mixed radix FFT, and the few vector iterations per call would only
     * be slow to compile.
     */
    private final boolean mVector;

    /**
     * Constructs an instance that uses the implementation selected by calibration.
     * @param size of the transform in real samples
     * @throws IllegalArgumentException if the size is not supported
     */
    public RealFFT(int size)
    {
        this(size, CalibrationManager.getInstance().getImplementation(CalibrationType.FFT_COMPLEX));
    }

    /**
     * Constructs an instance.
     * @param size of the transform in real samples
     * @param implementation of the complex FFT (see ComplexFFTFactory)
     * @throws IllegalArgumentException if the size or the implementation is not supported
     */
    public RealFFT(int size, Implementation implementation)
    {
        if(!isSupported(size))
        {
            throw new IllegalArgumentException("Unsupported real FFT size [" + size +
                "] - size must be even and half the size must have no prime factors other than 2, 3 and 5");
        }

        mSize = size;
        int half = size / 2;
        mFFT = ComplexFFTFactory.getComplexFFT(implementation, half, ComplexFFTFactory.Scalar.JTRANSFORMS);
        mCos = new float[half / 2 + 1];
        mSin = new float[half / 2 + 1];

        for(int k = 0; k < mCos.length; k++)
        {
            double angle = 2.0 * Math.PI * k / size;
            mCos[k] = (float)Math.cos(angle);
            mSin[k] = (float)Math.sin(angle);
        }

        //The scalar FFT of a power of two size is JTransforms, whose own real transform is the faster one
        mJTransforms = mFFT instanceof JTransformsComplexFFT ? new FloatFFT_1D(size) : null;

        mEven = new float[half];
        mOdd = new float[half];
        mVector = mFFT instanceof VectorComplexFFT && half >= VectorComplexFFT.MINIMUM_FOUR_STEP_SIZE;
    }

    /**
     * Indicates if the size can be transformed by this class.
     * @param size in real samples
     * @return true if the size is even and half the size has no prime factors other than 2, 3 and 5.
     */
    public static boolean isSupported(int size)
    {
        return size >= 2 && size % 2 == 0 && ComplexFFT.isSupported(size / 2);
    }

    /**
     * Size of this transform
     * @return size in real samples
     */
    public int getSize()
    {
        return mSize;
    }

    /**
     * Computes the forward DFT of real samples, in place, with the output layout of JTransforms realForward().
     * @param samples real, at least N in length
     */
    public void forward(float[] samples)
    {
        if(mJTransforms != null)
        {
            mJTransforms.realForward(samples);
            return;
        }

        final int half = mSize / 2;
        final float[] even = mEven;
        final float[] odd = mOdd;

        if(mVector)
        {
            VectorMoves.deinterleave(samples, 0, even, odd, 0, half);
            forward(even, odd);
            VectorMoves.interleave(even, odd, 0, 1.0f, samples, 0, half);
            return;
        }

        for(int n = 0; n < half; n++)
        {
            even[n] = samples[2 * n];
            odd[n] = samples[2 * n + 1];
        }

        forward(even, odd);

        for(int k = 0; k < half; k++)
        {
            samples[2 * k] = even[k];
            samples[2 * k + 1] = odd[k];
        }
    }

    /**
     * Computes the forward DFT of real samples that are split into the even and the odd samples, in place.
     * @param even samples x[2n], at least N/2 in length, which receive Re[k], with Re[0] at index 0
     * @param odd samples x[2n + 1], at least N/2 in length, which receive Im[k], with Re[N/2] at index 0
     */
    public void forward(float[] even, float[] odd)
    {
        mFFT.forward(even, odd);

        final int half = mSize / 2;
        final float[] cos = mCos;
        final float[] sin = mSin;

        float z0i = even[0];
        float z0q = odd[0];
        even[0] = z0i + z0q;
        odd[0] = z0i - z0q;

        int k = 1;

        if(mVector)
        {
            k = VectorMoves.realSplit(even, odd, cos, sin, half);
        }

        for(; k <= half / 2; k++)
        {
            int m = half - k;
            float zki = even[k];
            float zkq = odd[k];
            float zmi = even[m];
            float zmq = odd[m];

            //Even part e = (Z[k] + conj(Z[m])) / 2 and odd part o = (Z[k] - conj(Z[m])) / 2j
            float ei = 0.5f * (zki + zmi);
            float eq = 0.5f * (zkq - zmq);
            float oi = 0.5f * (zkq + zmq);
            float oq = -0.5f * (zki - zmi);

            //w o, where w = exp(-j2pi k/N)
            float wi = cos[k];
            float wq = -sin[k];
            float woi = oi * wi - oq * wq;
            float woq = oi * wq + oq * wi;

            //X[k] = e + w o and X[m] = conj(e - w o)
            even[k] = ei + woi;
            odd[k] = eq + woq;

            if(m != k)
            {
                even[m] = ei - woi;
                odd[m] = woq - eq;
            }
        }
    }
}
