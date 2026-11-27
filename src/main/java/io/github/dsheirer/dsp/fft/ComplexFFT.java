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

/**
 * Allocation-free complex FFT for sizes that factor into 2, 3 and 5 only.  Use ComplexFFTFactory to create the
 * implementation that calibration selected for this computer.
 *
 * Results match JTransforms FloatFFT_1D complexForward() and complexInverse(), within float rounding: the forward
 * transform uses the exp(-j...) kernel and is unscaled, and the inverse uses exp(+j...) and optionally scales by 1/N.
 *
 * Samples are interleaved complex: a[offset + 2k] = Re[k] and a[offset + 2k + 1] = Im[k] for 0 <= k < N.
 *
 * Note: implementations are NOT thread-safe since they reuse internal buffers.  Use one instance per thread.
 */
public abstract class ComplexFFT
{
    private final int mSize;
    private float[] mInterleaved;

    /**
     * Constructs an instance
     * @param size of the transform in complex samples.  Must be a positive value with no prime factors other than
     * 2, 3 and 5.
     * @throws IllegalArgumentException if the size is not supported
     */
    protected ComplexFFT(int size)
    {
        if(!isSupported(size))
        {
            throw new IllegalArgumentException("Unsupported FFT size [" + size +
                "] - size must be positive and have no prime factors other than 2, 3 and 5");
        }

        mSize = size;
    }

    /**
     * Indicates if the size can be transformed by this class.
     * @param size in complex samples
     * @return true if the size is positive and has no prime factors other than 2, 3 and 5.
     */
    public static boolean isSupported(int size)
    {
        return MixedRadixComplexFFT.isSupported(size);
    }

    /**
     * Size of this transform
     * @return size in complex samples
     */
    public int getSize()
    {
        return mSize;
    }

    /**
     * Computes the forward DFT of the interleaved complex samples, in place.  Equivalent to JTransforms
     * FloatFFT_1D.complexForward(samples, offset).
     * @param samples interleaved complex, with at least 2N values from the offset
     * @param offset to the first sample
     */
    public abstract void forward(float[] samples, int offset);

    /**
     * Computes the forward DFT of the interleaved complex samples, in place.
     * @param samples interleaved complex, with at least 2N values
     */
    public void forward(float[] samples)
    {
        forward(samples, 0);
    }

    /**
     * Computes the forward DFT of split complex samples, in place.  This implementation interleaves the samples
     * into a buffer that it allocates on first use.  Implementations that work on split samples transform them
     * directly, without a copy.
     * @param i inphase samples, at least N in length
     * @param q quadrature samples, at least N in length
     */
    public void forward(float[] i, float[] q)
    {
        if(mInterleaved == null)
        {
            mInterleaved = new float[2 * mSize];
        }

        final float[] samples = mInterleaved;

        for(int x = 0; x < mSize; x++)
        {
            samples[2 * x] = i[x];
            samples[2 * x + 1] = q[x];
        }

        forward(samples, 0);

        for(int x = 0; x < mSize; x++)
        {
            i[x] = samples[2 * x];
            q[x] = samples[2 * x + 1];
        }
    }

    /**
     * Computes the inverse DFT of the interleaved complex samples, in place.  Equivalent to JTransforms
     * FloatFFT_1D.complexInverse(samples, offset, scale).
     * @param samples interleaved complex, with at least 2N values from the offset
     * @param offset to the first sample
     * @param scale true to scale the results by 1/N
     */
    public abstract void inverse(float[] samples, int offset, boolean scale);

    /**
     * Computes the inverse DFT of the interleaved complex samples, in place.
     * @param samples interleaved complex, with at least 2N values
     * @param scale true to scale the results by 1/N
     */
    public void inverse(float[] samples, boolean scale)
    {
        inverse(samples, 0, scale);
    }

    /**
     * Computes the forward DFT of each block of a batch of contiguous blocks of interleaved complex samples, in place.
     * The result is the same as calling forward(samples, offset + 2N * b) for each block b, but implementations can
     * transform small sizes faster as a batch.
     * @param samples interleaved complex, with at least 2N x count values from the offset
     * @param offset to the first sample of the first block
     * @param count of blocks
     */
    public void forward(float[] samples, int offset, int count)
    {
        for(int block = 0; block < count; block++)
        {
            forward(samples, offset + 2 * mSize * block);
        }
    }

    /**
     * Computes the inverse DFT of each block of a batch of contiguous blocks of interleaved complex samples, in place.
     * The result is the same as calling inverse(samples, offset + 2N * b, scale) for each block b, but implementations
     * can transform small sizes faster as a batch.
     * @param samples interleaved complex, with at least 2N x count values from the offset
     * @param offset to the first sample of the first block
     * @param count of blocks
     * @param scale true to scale the results by 1/N
     */
    public void inverse(float[] samples, int offset, int count, boolean scale)
    {
        for(int block = 0; block < count; block++)
        {
            inverse(samples, offset + 2 * mSize * block, scale);
        }
    }
}
