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
 *
 * The transform kernels in this class are ported from the mixed radix complex FFT (cffti, cfftf and passf2/3/4/5)
 * in JTransforms org.jtransforms.fft.FloatFFT_1D, which carries the following license:
 *
 * JTransforms
 * Copyright (c) 2007 onward, Piotr Wendykier
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.github.dsheirer.dsp.fft;

/**
 * Allocation-free mixed radix complex FFT for sizes that factor into 2, 3 and 5 only.
 *
 * This is a port of the JTransforms FloatFFT_1D mixed radix complex transform path.  JTransforms allocates a new
 * 2N float scratch array on every mixed radix transform, which creates significant garbage collection pressure when
 * the transform runs at block rate (e.g. the polyphase channelizer IFFT).  This implementation pre-allocates the
 * scratch buffer once and reuses it for each transform.
 *
 * Results match JTransforms FloatFFT_1D complexForward() and complexInverse(): the forward transform uses the
 * exp(-j...) kernel, the inverse uses exp(+j...), and the inverse optionally scales by 1/N.  Twiddle factors are
 * calculated in double precision, so results can differ from JTransforms in the least significant bits.
 *
 * Samples are interleaved complex: a[offset + 2k] = Re[k] and a[offset + 2k + 1] = Im[k] for 0 <= k < N.
 *
 * Note: this class is NOT thread-safe since it reuses an internal scratch buffer.  Use one instance per thread.
 */
public class MixedRadixComplexFFT
{
    private static final int[] FACTORS = {4, 2, 3, 5};
    private static final int FORWARD = -1;
    private static final int INVERSE = 1;

    private final int mSize;
    private final int[] mFactors;
    private final float[] mTwiddles;
    private final float[] mScratch;
    private final float mScale;

    /**
     * Constructs an instance
     * @param size of the transform in complex samples.  Must be a positive value with no prime factors other than
     * 2, 3 and 5.
     * @throws IllegalArgumentException if the size is not supported
     */
    public MixedRadixComplexFFT(int size)
    {
        if(!isSupported(size))
        {
            throw new IllegalArgumentException("Unsupported FFT size [" + size +
                "] - size must be positive and have no prime factors other than 2, 3 and 5");
        }

        mSize = size;
        mFactors = factorize(size);
        mTwiddles = new float[2 * size + 2];
        mScratch = new float[2 * size];
        mScale = 1.0f / size;
        calculateTwiddles();
    }

    /**
     * Indicates if the size can be transformed by this class.
     * @param size in complex samples
     * @return true if the size is positive and has no prime factors other than 2, 3 and 5.
     */
    public static boolean isSupported(int size)
    {
        if(size < 1)
        {
            return false;
        }

        int remainder = size;

        for(int factor: new int[]{2, 3, 5})
        {
            while(remainder % factor == 0)
            {
                remainder /= factor;
            }
        }

        return remainder == 1;
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
    public void forward(float[] samples, int offset)
    {
        transform(samples, offset, FORWARD);
    }

    /**
     * Computes the forward DFT of the interleaved complex samples, in place.
     * @param samples interleaved complex, with at least 2N values
     */
    public void forward(float[] samples)
    {
        forward(samples, 0);
    }

    /**
     * Computes the inverse DFT of the interleaved complex samples, in place.  Equivalent to JTransforms
     * FloatFFT_1D.complexInverse(samples, offset, scale).
     * @param samples interleaved complex, with at least 2N values from the offset
     * @param offset to the first sample
     * @param scale true to scale the results by 1/N
     */
    public void inverse(float[] samples, int offset, boolean scale)
    {
        transform(samples, offset, INVERSE);

        if(scale && mSize > 1)
        {
            int end = offset + 2 * mSize;

            for(int x = offset; x < end; x++)
            {
                samples[x] *= mScale;
            }
        }
    }

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
     * Factors the size into the radix sequence used by the transform, ported from JTransforms cffti().  Radix 4 is
     * preferred, then 2, 3 and 5, and a radix 2 factor is moved to the front of the sequence.
     */
    private static int[] factorize(int size)
    {
        if(size == 1)
        {
            return new int[0];
        }

        int[] factors = new int[32];
        int count = 0;
        int remainder = size;

        for(int factor: FACTORS)
        {
            while(remainder % factor == 0)
            {
                factors[count++] = factor;
                remainder /= factor;

                if(factor == 2 && count != 1)
                {
                    System.arraycopy(factors, 0, factors, 1, count - 1);
                    factors[0] = 2;
                }
            }
        }

        if(remainder != 1)
        {
            throw new IllegalArgumentException("Unsupported FFT size [" + size + "]");
        }

        int[] result = new int[count];
        System.arraycopy(factors, 0, result, 0, count);
        return result;
    }

    /**
     * Calculates the twiddle factor table, ported from JTransforms cffti() with the table offset removed and the
     * angles calculated in double precision.
     */
    private void calculateTwiddles()
    {
        if(mSize == 1)
        {
            return;
        }

        double angleIncrement = 2.0 * Math.PI / mSize;
        int i = 1;
        int l1 = 1;

        for(int factor: mFactors)
        {
            int ld = 0;
            int l2 = l1 * factor;
            int ido = mSize / l2;
            int idot = ido + ido + 2;

            for(int j = 1; j < factor; j++)
            {
                mTwiddles[i - 1] = 1;
                mTwiddles[i] = 0;
                ld += l1;
                int fi = 0;
                double argld = ld * angleIncrement;

                for(int ii = 4; ii <= idot; ii += 2)
                {
                    i += 2;
                    fi++;
                    double arg = fi * argld;
                    mTwiddles[i - 1] = (float)Math.cos(arg);
                    mTwiddles[i] = (float)Math.sin(arg);
                }
            }

            l1 = l2;
        }
    }

    /**
     * Performs the transform, ported from JTransforms cfftf() using the reusable scratch buffer.
     * @param a samples
     * @param offa offset to first sample
     * @param isign -1 for forward and +1 for inverse
     */
    private void transform(float[] a, int offa, int isign)
    {
        if(mSize == 1)
        {
            return;
        }

        float[] ch = mScratch;
        int twon = 2 * mSize;
        boolean inScratch = false;
        int l1 = 1;
        int iw = 0;

        for(int factor: mFactors)
        {
            int l2 = factor * l1;
            int idot = 2 * (mSize / l2);

            switch(factor)
            {
                case 4:
                    if(inScratch)
                    {
                        passf4(idot, l1, ch, 0, a, offa, iw, isign);
                    }
                    else
                    {
                        passf4(idot, l1, a, offa, ch, 0, iw, isign);
                    }
                    break;
                case 2:
                    if(inScratch)
                    {
                        passf2(idot, l1, ch, 0, a, offa, iw, isign);
                    }
                    else
                    {
                        passf2(idot, l1, a, offa, ch, 0, iw, isign);
                    }
                    break;
                case 3:
                    if(inScratch)
                    {
                        passf3(idot, l1, ch, 0, a, offa, iw, isign);
                    }
                    else
                    {
                        passf3(idot, l1, a, offa, ch, 0, iw, isign);
                    }
                    break;
                case 5:
                    if(inScratch)
                    {
                        passf5(idot, l1, ch, 0, a, offa, iw, isign);
                    }
                    else
                    {
                        passf5(idot, l1, a, offa, ch, 0, iw, isign);
                    }
                    break;
                default:
                    throw new IllegalStateException("Unsupported radix: " + factor);
            }

            inScratch = !inScratch;
            l1 = l2;
            iw += (factor - 1) * idot;
        }

        if(inScratch)
        {
            System.arraycopy(ch, 0, a, offa, twon);
        }
    }

    /**
     * Radix 2 butterfly, ported from JTransforms passf2().
     */
    private void passf2(final int ido, final int l1, final float[] in, final int in_off, final float[] out,
                        final int out_off, final int offset, final int isign)
    {
        final float[] wtable = mTwiddles;
        int iw1 = offset;
        int idx = ido * l1;

        if(ido <= 2)
        {
            for(int k = 0; k < l1; k++)
            {
                int idx0 = k * ido;
                int iidx1 = in_off + 2 * idx0;
                int iidx2 = iidx1 + ido;
                float a1r = in[iidx1];
                float a1i = in[iidx1 + 1];
                float a2r = in[iidx2];
                float a2i = in[iidx2 + 1];

                int oidx1 = out_off + idx0;
                int oidx2 = oidx1 + idx;
                out[oidx1] = a1r + a2r;
                out[oidx1 + 1] = a1i + a2i;
                out[oidx2] = a1r - a2r;
                out[oidx2 + 1] = a1i - a2i;
            }
        }
        else
        {
            for(int k = 0; k < l1; k++)
            {
                for(int i = 0; i < ido - 1; i += 2)
                {
                    int idx0 = k * ido;
                    int iidx1 = in_off + i + 2 * idx0;
                    int iidx2 = iidx1 + ido;
                    float i1r = in[iidx1];
                    float i1i = in[iidx1 + 1];
                    float i2r = in[iidx2];
                    float i2i = in[iidx2 + 1];

                    int widx1 = i + iw1;
                    float w1r = wtable[widx1];
                    float w1i = isign * wtable[widx1 + 1];

                    float t1r = i1r - i2r;
                    float t1i = i1i - i2i;

                    int oidx1 = out_off + i + idx0;
                    int oidx2 = oidx1 + idx;
                    out[oidx1] = i1r + i2r;
                    out[oidx1 + 1] = i1i + i2i;
                    out[oidx2] = w1r * t1r - w1i * t1i;
                    out[oidx2 + 1] = w1r * t1i + w1i * t1r;
                }
            }
        }
    }

    /**
     * Radix 3 butterfly, ported from JTransforms passf3().
     */
    private void passf3(final int ido, final int l1, final float[] in, final int in_off, final float[] out,
                        final int out_off, final int offset, final int isign)
    {
        final float taur = -0.5f;
        final float taui = 0.866025403784438707610604524234076962f;
        final float[] wtable = mTwiddles;
        int iw1 = offset;
        int iw2 = iw1 + ido;
        final int idxt = l1 * ido;

        if(ido == 2)
        {
            for(int k = 1; k <= l1; k++)
            {
                int iidx1 = in_off + (3 * k - 2) * ido;
                int iidx2 = iidx1 + ido;
                int iidx3 = iidx1 - ido;
                float i1r = in[iidx1];
                float i1i = in[iidx1 + 1];
                float i2r = in[iidx2];
                float i2i = in[iidx2 + 1];
                float i3r = in[iidx3];
                float i3i = in[iidx3 + 1];

                float tr2 = i1r + i2r;
                float cr2 = i3r + taur * tr2;
                float ti2 = i1i + i2i;
                float ci2 = i3i + taur * ti2;
                float cr3 = isign * taui * (i1r - i2r);
                float ci3 = isign * taui * (i1i - i2i);

                int oidx1 = out_off + (k - 1) * ido;
                int oidx2 = oidx1 + idxt;
                int oidx3 = oidx2 + idxt;
                out[oidx1] = i3r + tr2;
                out[oidx1 + 1] = i3i + ti2;
                out[oidx2] = cr2 - ci3;
                out[oidx2 + 1] = ci2 + cr3;
                out[oidx3] = cr2 + ci3;
                out[oidx3 + 1] = ci2 - cr3;
            }
        }
        else
        {
            for(int k = 1; k <= l1; k++)
            {
                int idx1 = in_off + (3 * k - 2) * ido;
                int idx2 = out_off + (k - 1) * ido;

                for(int i = 0; i < ido - 1; i += 2)
                {
                    int iidx1 = i + idx1;
                    int iidx2 = iidx1 + ido;
                    int iidx3 = iidx1 - ido;
                    float a1r = in[iidx1];
                    float a1i = in[iidx1 + 1];
                    float a2r = in[iidx2];
                    float a2i = in[iidx2 + 1];
                    float a3r = in[iidx3];
                    float a3i = in[iidx3 + 1];

                    float tr2 = a1r + a2r;
                    float cr2 = a3r + taur * tr2;
                    float ti2 = a1i + a2i;
                    float ci2 = a3i + taur * ti2;
                    float cr3 = isign * taui * (a1r - a2r);
                    float ci3 = isign * taui * (a1i - a2i);
                    float dr2 = cr2 - ci3;
                    float dr3 = cr2 + ci3;
                    float di2 = ci2 + cr3;
                    float di3 = ci2 - cr3;

                    int widx1 = i + iw1;
                    int widx2 = i + iw2;
                    float w1r = wtable[widx1];
                    float w1i = isign * wtable[widx1 + 1];
                    float w2r = wtable[widx2];
                    float w2i = isign * wtable[widx2 + 1];

                    int oidx1 = i + idx2;
                    int oidx2 = oidx1 + idxt;
                    int oidx3 = oidx2 + idxt;
                    out[oidx1] = a3r + tr2;
                    out[oidx1 + 1] = a3i + ti2;
                    out[oidx2] = w1r * dr2 - w1i * di2;
                    out[oidx2 + 1] = w1r * di2 + w1i * dr2;
                    out[oidx3] = w2r * dr3 - w2i * di3;
                    out[oidx3 + 1] = w2r * di3 + w2i * dr3;
                }
            }
        }
    }

    /**
     * Radix 4 butterfly, ported from JTransforms passf4().
     */
    private void passf4(final int ido, final int l1, final float[] in, final int in_off, final float[] out,
                        final int out_off, final int offset, final int isign)
    {
        final float[] wtable = mTwiddles;
        int iw1 = offset;
        int iw2 = iw1 + ido;
        int iw3 = iw2 + ido;
        int idx0 = l1 * ido;

        if(ido == 2)
        {
            for(int k = 0; k < l1; k++)
            {
                int idxt1 = k * ido;
                int iidx1 = in_off + 4 * idxt1 + 1;
                int iidx2 = iidx1 + ido;
                int iidx3 = iidx2 + ido;
                int iidx4 = iidx3 + ido;

                float i1i = in[iidx1 - 1];
                float i1r = in[iidx1];
                float i2i = in[iidx2 - 1];
                float i2r = in[iidx2];
                float i3i = in[iidx3 - 1];
                float i3r = in[iidx3];
                float i4i = in[iidx4 - 1];
                float i4r = in[iidx4];

                float ti1 = i1r - i3r;
                float ti2 = i1r + i3r;
                float tr4 = i4r - i2r;
                float ti3 = i2r + i4r;
                float tr1 = i1i - i3i;
                float tr2 = i1i + i3i;
                float ti4 = i2i - i4i;
                float tr3 = i2i + i4i;

                int oidx1 = out_off + idxt1;
                int oidx2 = oidx1 + idx0;
                int oidx3 = oidx2 + idx0;
                int oidx4 = oidx3 + idx0;
                out[oidx1] = tr2 + tr3;
                out[oidx1 + 1] = ti2 + ti3;
                out[oidx2] = tr1 + isign * tr4;
                out[oidx2 + 1] = ti1 + isign * ti4;
                out[oidx3] = tr2 - tr3;
                out[oidx3 + 1] = ti2 - ti3;
                out[oidx4] = tr1 - isign * tr4;
                out[oidx4 + 1] = ti1 - isign * ti4;
            }
        }
        else
        {
            for(int k = 0; k < l1; k++)
            {
                int idx1 = k * ido;
                int idx2 = in_off + 1 + 4 * idx1;

                for(int i = 0; i < ido - 1; i += 2)
                {
                    int iidx1 = i + idx2;
                    int iidx2 = iidx1 + ido;
                    int iidx3 = iidx2 + ido;
                    int iidx4 = iidx3 + ido;
                    float i1i = in[iidx1 - 1];
                    float i1r = in[iidx1];
                    float i2i = in[iidx2 - 1];
                    float i2r = in[iidx2];
                    float i3i = in[iidx3 - 1];
                    float i3r = in[iidx3];
                    float i4i = in[iidx4 - 1];
                    float i4r = in[iidx4];

                    float ti1 = i1r - i3r;
                    float ti2 = i1r + i3r;
                    float ti3 = i2r + i4r;
                    float tr4 = i4r - i2r;
                    float tr1 = i1i - i3i;
                    float tr2 = i1i + i3i;
                    float ti4 = i2i - i4i;
                    float tr3 = i2i + i4i;
                    float cr3 = tr2 - tr3;
                    float ci3 = ti2 - ti3;
                    float cr2 = tr1 + isign * tr4;
                    float cr4 = tr1 - isign * tr4;
                    float ci2 = ti1 + isign * ti4;
                    float ci4 = ti1 - isign * ti4;

                    int widx1 = i + iw1;
                    int widx2 = i + iw2;
                    int widx3 = i + iw3;
                    float w1r = wtable[widx1];
                    float w1i = isign * wtable[widx1 + 1];
                    float w2r = wtable[widx2];
                    float w2i = isign * wtable[widx2 + 1];
                    float w3r = wtable[widx3];
                    float w3i = isign * wtable[widx3 + 1];

                    int oidx1 = out_off + i + idx1;
                    int oidx2 = oidx1 + idx0;
                    int oidx3 = oidx2 + idx0;
                    int oidx4 = oidx3 + idx0;
                    out[oidx1] = tr2 + tr3;
                    out[oidx1 + 1] = ti2 + ti3;
                    out[oidx2] = w1r * cr2 - w1i * ci2;
                    out[oidx2 + 1] = w1r * ci2 + w1i * cr2;
                    out[oidx3] = w2r * cr3 - w2i * ci3;
                    out[oidx3 + 1] = w2r * ci3 + w2i * cr3;
                    out[oidx4] = w3r * cr4 - w3i * ci4;
                    out[oidx4 + 1] = w3r * ci4 + w3i * cr4;
                }
            }
        }
    }

    /**
     * Radix 5 butterfly, ported from JTransforms passf5().
     */
    private void passf5(final int ido, final int l1, final float[] in, final int in_off, final float[] out,
                        final int out_off, final int offset, final int isign)
    {
        final float tr11 = 0.309016994374947451262869435595348477f;
        final float ti11 = 0.951056516295153531181938433292089030f;
        final float tr12 = -0.809016994374947340240566973079694435f;
        final float ti12 = 0.587785252292473248125759255344746634f;
        final float[] wtable = mTwiddles;
        int iw1 = offset;
        int iw2 = iw1 + ido;
        int iw3 = iw2 + ido;
        int iw4 = iw3 + ido;
        int idx0 = l1 * ido;

        if(ido == 2)
        {
            for(int k = 1; k <= l1; ++k)
            {
                int iidx1 = in_off + (5 * k - 4) * ido + 1;
                int iidx2 = iidx1 + ido;
                int iidx3 = iidx1 - ido;
                int iidx4 = iidx2 + ido;
                int iidx5 = iidx4 + ido;

                float i1i = in[iidx1 - 1];
                float i1r = in[iidx1];
                float i2i = in[iidx2 - 1];
                float i2r = in[iidx2];
                float i3i = in[iidx3 - 1];
                float i3r = in[iidx3];
                float i4i = in[iidx4 - 1];
                float i4r = in[iidx4];
                float i5i = in[iidx5 - 1];
                float i5r = in[iidx5];

                float ti5 = i1r - i5r;
                float ti2 = i1r + i5r;
                float ti4 = i2r - i4r;
                float ti3 = i2r + i4r;
                float tr5 = i1i - i5i;
                float tr2 = i1i + i5i;
                float tr4 = i2i - i4i;
                float tr3 = i2i + i4i;
                float cr2 = i3i + tr11 * tr2 + tr12 * tr3;
                float ci2 = i3r + tr11 * ti2 + tr12 * ti3;
                float cr3 = i3i + tr12 * tr2 + tr11 * tr3;
                float ci3 = i3r + tr12 * ti2 + tr11 * ti3;
                float cr5 = isign * (ti11 * tr5 + ti12 * tr4);
                float ci5 = isign * (ti11 * ti5 + ti12 * ti4);
                float cr4 = isign * (ti12 * tr5 - ti11 * tr4);
                float ci4 = isign * (ti12 * ti5 - ti11 * ti4);

                int oidx1 = out_off + (k - 1) * ido;
                int oidx2 = oidx1 + idx0;
                int oidx3 = oidx2 + idx0;
                int oidx4 = oidx3 + idx0;
                int oidx5 = oidx4 + idx0;
                out[oidx1] = i3i + tr2 + tr3;
                out[oidx1 + 1] = i3r + ti2 + ti3;
                out[oidx2] = cr2 - ci5;
                out[oidx2 + 1] = ci2 + cr5;
                out[oidx3] = cr3 - ci4;
                out[oidx3 + 1] = ci3 + cr4;
                out[oidx4] = cr3 + ci4;
                out[oidx4 + 1] = ci3 - cr4;
                out[oidx5] = cr2 + ci5;
                out[oidx5 + 1] = ci2 - cr5;
            }
        }
        else
        {
            for(int k = 1; k <= l1; k++)
            {
                int idx1 = in_off + 1 + (k * 5 - 4) * ido;
                int idx2 = out_off + (k - 1) * ido;

                for(int i = 0; i < ido - 1; i += 2)
                {
                    int iidx1 = i + idx1;
                    int iidx2 = iidx1 + ido;
                    int iidx3 = iidx1 - ido;
                    int iidx4 = iidx2 + ido;
                    int iidx5 = iidx4 + ido;
                    float i1i = in[iidx1 - 1];
                    float i1r = in[iidx1];
                    float i2i = in[iidx2 - 1];
                    float i2r = in[iidx2];
                    float i3i = in[iidx3 - 1];
                    float i3r = in[iidx3];
                    float i4i = in[iidx4 - 1];
                    float i4r = in[iidx4];
                    float i5i = in[iidx5 - 1];
                    float i5r = in[iidx5];

                    float ti5 = i1r - i5r;
                    float ti2 = i1r + i5r;
                    float ti4 = i2r - i4r;
                    float ti3 = i2r + i4r;
                    float tr5 = i1i - i5i;
                    float tr2 = i1i + i5i;
                    float tr4 = i2i - i4i;
                    float tr3 = i2i + i4i;
                    float cr2 = i3i + tr11 * tr2 + tr12 * tr3;
                    float ci2 = i3r + tr11 * ti2 + tr12 * ti3;
                    float cr3 = i3i + tr12 * tr2 + tr11 * tr3;
                    float ci3 = i3r + tr12 * ti2 + tr11 * ti3;
                    float cr5 = isign * (ti11 * tr5 + ti12 * tr4);
                    float ci5 = isign * (ti11 * ti5 + ti12 * ti4);
                    float cr4 = isign * (ti12 * tr5 - ti11 * tr4);
                    float ci4 = isign * (ti12 * ti5 - ti11 * ti4);
                    float dr3 = cr3 - ci4;
                    float dr4 = cr3 + ci4;
                    float di3 = ci3 + cr4;
                    float di4 = ci3 - cr4;
                    float dr5 = cr2 + ci5;
                    float dr2 = cr2 - ci5;
                    float di5 = ci2 - cr5;
                    float di2 = ci2 + cr5;

                    int widx1 = i + iw1;
                    int widx2 = i + iw2;
                    int widx3 = i + iw3;
                    int widx4 = i + iw4;
                    float w1r = wtable[widx1];
                    float w1i = isign * wtable[widx1 + 1];
                    float w2r = wtable[widx2];
                    float w2i = isign * wtable[widx2 + 1];
                    float w3r = wtable[widx3];
                    float w3i = isign * wtable[widx3 + 1];
                    float w4r = wtable[widx4];
                    float w4i = isign * wtable[widx4 + 1];

                    int oidx1 = i + idx2;
                    int oidx2 = oidx1 + idx0;
                    int oidx3 = oidx2 + idx0;
                    int oidx4 = oidx3 + idx0;
                    int oidx5 = oidx4 + idx0;
                    out[oidx1] = i3i + tr2 + tr3;
                    out[oidx1 + 1] = i3r + ti2 + ti3;
                    out[oidx2] = w1r * dr2 - w1i * di2;
                    out[oidx2 + 1] = w1r * di2 + w1i * dr2;
                    out[oidx3] = w2r * dr3 - w2i * di3;
                    out[oidx3 + 1] = w2r * di3 + w2i * dr3;
                    out[oidx4] = w3r * dr4 - w3i * di4;
                    out[oidx4 + 1] = w3r * di4 + w3i * dr4;
                    out[oidx5] = w4r * dr5 - w4i * di5;
                    out[oidx5 + 1] = w4r * di5 + w4i * dr5;
                }
            }
        }
    }
}
