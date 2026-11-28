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

import org.jtransforms.fft.FloatFFT_1D;

/**
 * Scalar complex FFT for power of two sizes: the JTransforms split radix FFT, which is faster at these sizes than the
 * mixed radix FFT.
 *
 * Note: JTransforms uses its own thread pool for sizes of 4096 and up, and allocates for each of those transforms.
 */
public class JTransformsComplexFFT extends ComplexFFT
{
    private final FloatFFT_1D mFFT;

    /**
     * Constructs an instance
     * @param size of the transform in complex samples, a power of two
     */
    public JTransformsComplexFFT(int size)
    {
        super(size);
        mFFT = new FloatFFT_1D(size);
    }

    @Override
    public void forward(float[] samples, int offset)
    {
        mFFT.complexForward(samples, offset);
    }

    @Override
    public void inverse(float[] samples, int offset, boolean scale)
    {
        mFFT.complexInverse(samples, offset, scale);
    }
}
