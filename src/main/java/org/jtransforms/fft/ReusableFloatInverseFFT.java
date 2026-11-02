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
package org.jtransforms.fft;

import java.util.ArrayList;
import java.util.List;

/**
 * Scaled complex inverse FFT that produces the same values as FloatFFT_1D.complexInverse(samples, offset, true) without
 * allocating on each call.
 *
 * FloatFFT_1D allocates a scratch array of two floats per channel on every mixed-radix transform.  This class runs the
 * same mixed-radix passes using a single reused scratch array.  It's in this package because those passes are package
 * private.  Sizes that FloatFFT_1D doesn't transform as mixed-radix (powers of two and sizes with other prime factors)
 * are delegated to FloatFFT_1D.
 *
 * Note: this class isn't thread safe because the scratch array is shared across calls.
 */
public class ReusableFloatInverseFFT
{
    private static final int[] SUPPORTED_RADIX = {3, 5};
    private final FloatFFT_1D mFFT;
    private final int mSize;
    private final int[] mFactors;
    private final float[] mScratch;
    private final float mScale;

    /**
     * Constructs an instance
     * @param size of the transform in complex samples
     */
    public ReusableFloatInverseFFT(int size)
    {
        mSize = size;
        mFFT = new FloatFFT_1D(size);
        mFactors = getFactors(size);
        mScratch = mFactors != null ? new float[2 * size] : null;
        mScale = 1.0f / size;
    }

    /**
     * Identifies the order of radix passes for the size, matching the order used by FloatFFT_1D.
     * @param size of the transform
     * @return radix values in pass order, or null if the size should be delegated to FloatFFT_1D.
     */
    private static int[] getFactors(int size)
    {
        if(size < 2 || Integer.bitCount(size) == 1)
        {
            return null;
        }

        List<Integer> factors = new ArrayList<>();
        int remaining = size;

        while(remaining % 4 == 0)
        {
            factors.add(4);
            remaining /= 4;
        }

        //FloatFFT_1D moves a radix-2 pass to the front
        if(remaining % 2 == 0)
        {
            factors.addFirst(2);
            remaining /= 2;
        }

        for(int radix : SUPPORTED_RADIX)
        {
            while(remaining % radix == 0)
            {
                factors.add(radix);
                remaining /= radix;
            }
        }

        if(remaining != 1)
        {
            return null;
        }

        return factors.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Performs a scaled inverse transform in place.
     * @param samples containing interleaved I/Q values
     * @param offset to the start of the block in the samples array
     */
    public void complexInverse(float[] samples, int offset)
    {
        if(mFactors == null)
        {
            mFFT.complexInverse(samples, offset, true);
            return;
        }

        final float[] scratch = mScratch;
        final int size = mSize;
        boolean resultsInScratch = false;
        int subTransforms = 1;
        int twiddleOffset = 2 * size;

        for(int radix : mFactors)
        {
            int next = radix * subTransforms;
            int stride = 2 * (size / next);
            float[] in = resultsInScratch ? scratch : samples;
            int inOffset = resultsInScratch ? 0 : offset;
            float[] out = resultsInScratch ? samples : scratch;
            int outOffset = resultsInScratch ? offset : 0;

            switch(radix)
            {
                case 2 -> mFFT.passf2(stride, subTransforms, in, inOffset, out, outOffset, twiddleOffset, 1);
                case 3 -> mFFT.passf3(stride, subTransforms, in, inOffset, out, outOffset, twiddleOffset, 1);
                case 4 -> mFFT.passf4(stride, subTransforms, in, inOffset, out, outOffset, twiddleOffset, 1);
                default -> mFFT.passf5(stride, subTransforms, in, inOffset, out, outOffset, twiddleOffset, 1);
            }

            resultsInScratch = !resultsInScratch;
            subTransforms = next;
            twiddleOffset += (radix - 1) * stride;
        }

        if(resultsInScratch)
        {
            System.arraycopy(scratch, 0, samples, offset, 2 * size);
        }

        final float scale = mScale;
        final int end = offset + 2 * size;

        for(int x = offset; x < end; x++)
        {
            samples[x] *= scale;
        }
    }
}
