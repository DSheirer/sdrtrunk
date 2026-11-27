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

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Batch of equal length forward complex FFTs over split (separate inphase and quadrature) arrays, one FFT per column
 * of a row-major matrix, where each FFT runs down a column.  Every butterfly operates on whole rows, so the inner
 * loops run over contiguous rows no matter the stage, and are vectorized across the columns with the CPU's preferred
 * vector width.  The number of columns is always a whole number of vectors.
 *
 * Self-sorting (Stockham) decimation in frequency stages of radix 4, 2, 5 and 3 move the data between the caller's
 * arrays and work strips, so the output is in natural order with no bit reversal.  The forward transform uses the
 * exp(-j2pi nk/N) kernel and is unscaled.  Twiddle factors are calculated in double precision.
 *
 * Instances keep no per-transform state and allocate nothing per transform: the caller provides the work strips.
 */
class ColumnFFT
{
    private static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;
    private static final int LANES = SPECIES.length();
    private static final double TWO_PI = 2.0 * Math.PI;
    private static final float C3 = -0.5f;
    private static final float S3 = (float)Math.sin(TWO_PI / 3.0);
    private static final float C51 = (float)Math.cos(TWO_PI / 5.0);
    private static final float C52 = (float)Math.cos(2.0 * TWO_PI / 5.0);
    private static final float S51 = (float)Math.sin(TWO_PI / 5.0);
    private static final float S52 = (float)Math.sin(2.0 * TWO_PI / 5.0);

    private final int mLength;
    private final int[] mRadix;
    private final int[] mStride;
    private final int[] mGroups;
    private final float[][] mTwiddleI;
    private final float[][] mTwiddleQ;

    /**
     * Constructs an instance.
     * @param length of each FFT (number of rows), with no prime factors other than 2, 3 and 5
     * @throws IllegalArgumentException if the length is not supported
     */
    public ColumnFFT(int length)
    {
        if(!MixedRadixComplexFFT.isSupported(length))
        {
            throw new IllegalArgumentException("Unsupported FFT length [" + length +
                "] - length must be positive and have no prime factors other than 2, 3 and 5");
        }

        mLength = length;

        int count = 0;

        for(int remaining = length; remaining > 1; remaining /= getRadix(remaining))
        {
            count++;
        }

        mRadix = new int[count];
        mStride = new int[count];
        mGroups = new int[count];
        mTwiddleI = new float[count][];
        mTwiddleQ = new float[count][];

        int remaining = length;
        int stride = 1;

        for(int stage = 0; stage < count; stage++)
        {
            int radix = getRadix(remaining);
            int groups = remaining / radix;
            mRadix[stage] = radix;
            mStride[stage] = stride;
            mGroups[stage] = groups;
            mTwiddleI[stage] = new float[groups * radix];
            mTwiddleQ[stage] = new float[groups * radix];

            for(int j = 0; j < groups; j++)
            {
                for(int k = 0; k < radix; k++)
                {
                    double angle = -TWO_PI * j * k / remaining;
                    mTwiddleI[stage][j * radix + k] = (float)Math.cos(angle);
                    mTwiddleQ[stage][j * radix + k] = (float)Math.sin(angle);
                }
            }

            stride *= radix;
            remaining = groups;
        }
    }

    /**
     * Radix of the next stage: 4 is preferred, then 2, 5 and 3.
     */
    private static int getRadix(int remaining)
    {
        if(remaining % 4 == 0)
        {
            return 4;
        }
        else if(remaining % 2 == 0)
        {
            return 2;
        }
        else if(remaining % 5 == 0)
        {
            return 5;
        }

        return 3;
    }

    /**
     * Length of each FFT, which is the number of rows.
     */
    public int getLength()
    {
        return mLength;
    }

    /**
     * Number of stages, which is the number of times the data moves between arrays.
     */
    public int getStageCount()
    {
        return mRadix.length;
    }

    /**
     * Transforms width columns that start at a base offset within the rows of a larger matrix, reading the source and
     * writing the destination directly, with the intermediate stages in two of the work strips.
     * @param si source inphase
     * @param sq source quadrature
     * @param sourceBase offset of the first column in row 0 of the source
     * @param sourceStride row length of the source
     * @param di destination inphase, which must not be one of the work strips, and may be the source only when there
     * are two or more stages
     * @param dq destination quadrature
     * @param destinationBase offset of the first column in row 0 of the destination
     * @param destinationStride row length of the destination
     * @param width number of columns (FFTs), a whole number of vectors
     * @param work four work strips (ai, aq, bi, bq), each at least length x width
     */
    public void transform(float[] si, float[] sq, int sourceBase, int sourceStride, float[] di, float[] dq,
                          int destinationBase, int destinationStride, int width, float[][] work)
    {
        int last = mRadix.length - 1;

        if(last < 0)
        {
            copy(si, sq, sourceBase, sourceStride, di, dq, destinationBase, destinationStride, width);
            return;
        }

        float[] xi = si;
        float[] xq = sq;
        int xBase = sourceBase;
        int xStride = sourceStride;

        for(int stage = 0; stage <= last; stage++)
        {
            float[] yi;
            float[] yq;
            int yBase;
            int yStride;

            if(stage == last)
            {
                yi = di;
                yq = dq;
                yBase = destinationBase;
                yStride = destinationStride;
            }
            else
            {
                yi = work[(stage & 1) * 2];
                yq = work[(stage & 1) * 2 + 1];
                yBase = 0;
                yStride = width;
            }

            process(stage, xi, xq, xBase, xStride, yi, yq, yBase, yStride, width);
            xi = yi;
            xq = yq;
            xBase = yBase;
            xStride = yStride;
        }
    }

    /**
     * Transforms width contiguous columns (a length x width matrix) in place.
     * @param i inphase matrix, which must not be one of the work strips
     * @param q quadrature matrix
     * @param width number of columns (FFTs), a whole number of vectors
     * @param work four work strips, each at least length x width
     */
    public void transform(float[] i, float[] q, int width, float[][] work)
    {
        if(mRadix.length == 1)
        {
            //Stockham stages can't run in place, so the single stage writes to a work strip that is copied back
            process(0, i, q, 0, width, work[0], work[1], 0, width, width);
            System.arraycopy(work[0], 0, i, 0, mLength * width);
            System.arraycopy(work[1], 0, q, 0, mLength * width);
        }
        else
        {
            transform(i, q, 0, width, i, q, 0, width, width, work);
        }
    }

    /**
     * Multiplies length complex values by a table of complex values, in place: x[offset + n] *= w[wOffset + n].
     * @param xi inphase values
     * @param xq quadrature values
     * @param offset to the first value
     * @param wi inphase of the table
     * @param wq quadrature of the table
     * @param wOffset to the first table value
     * @param length number of values, a whole number of vectors
     */
    public void multiply(float[] xi, float[] xq, int offset, float[] wi, float[] wq, int wOffset, int length)
    {
        for(int n = 0; n < length; n += LANES)
        {
            FloatVector ai = FloatVector.fromArray(SPECIES, xi, offset + n);
            FloatVector aq = FloatVector.fromArray(SPECIES, xq, offset + n);
            FloatVector bi = FloatVector.fromArray(SPECIES, wi, wOffset + n);
            FloatVector bq = FloatVector.fromArray(SPECIES, wq, wOffset + n);
            ai.mul(bi).sub(aq.mul(bq)).intoArray(xi, offset + n);
            ai.mul(bq).add(aq.mul(bi)).intoArray(xq, offset + n);
        }
    }

    private void copy(float[] si, float[] sq, int sourceBase, int sourceStride, float[] di, float[] dq,
                      int destinationBase, int destinationStride, int width)
    {
        for(int row = 0; row < mLength; row++)
        {
            System.arraycopy(si, sourceBase + row * sourceStride, di, destinationBase + row * destinationStride,
                width);
            System.arraycopy(sq, sourceBase + row * sourceStride, dq, destinationBase + row * destinationStride,
                width);
        }
    }

    /**
     * One Stockham decimation in frequency stage: element k of each group of radix input rows spaced
     * groups * stride rows apart is transformed, multiplied by the twiddle exp(-j2pi jk/n) and written to adjacent
     * groups of output rows.
     */
    private void process(int stage, float[] xi, float[] xq, int xBase, int xStride, float[] yi, float[] yq, int yBase,
                         int yStride, int width)
    {
        final int radix = mRadix[stage];
        final int stride = mStride[stage];
        final int groups = mGroups[stage];
        final float[] twi = mTwiddleI[stage];
        final float[] twq = mTwiddleQ[stage];
        final int inputSpacing = groups * stride * xStride;
        final int outputSpacing = stride * yStride;

        for(int j = 0; j < groups; j++)
        {
            int t = j * radix;

            for(int s = 0; s < stride; s++)
            {
                int in = xBase + (s + stride * j) * xStride;
                int out = yBase + (s + stride * radix * j) * yStride;

                switch(radix)
                {
                    case 4 ->
                    {
                        if(j == 0)
                        {
                            radix4(xi, xq, yi, yq, in, inputSpacing, out, outputSpacing, width);
                        }
                        else
                        {
                            radix4(xi, xq, yi, yq, in, inputSpacing, out, outputSpacing, width, twi[t + 1],
                                twq[t + 1], twi[t + 2], twq[t + 2], twi[t + 3], twq[t + 3]);
                        }
                    }
                    case 2 -> radix2(xi, xq, yi, yq, in, inputSpacing, out, outputSpacing, width, twi[t + 1],
                        twq[t + 1]);
                    case 5 -> radix5(xi, xq, yi, yq, in, inputSpacing, out, outputSpacing, width, twi[t + 1],
                        twq[t + 1], twi[t + 2], twq[t + 2], twi[t + 3], twq[t + 3], twi[t + 4], twq[t + 4]);
                    default -> radix3(xi, xq, yi, yq, in, inputSpacing, out, outputSpacing, width, twi[t + 1],
                        twq[t + 1], twi[t + 2], twq[t + 2]);
                }
            }
        }
    }

    /**
     * Radix 2 butterfly of width columns.  Inputs are rows in and in + is, outputs are rows out and out + os, and w1
     * is the twiddle for output 1.
     */
    private void radix2(float[] xi, float[] xq, float[] yi, float[] yq, int in, int is, int out, int os, int width, float w1i, float w1q)
    {
        int i1 = in + is;
        int o1 = out + os;
        for(int c = 0; c < width; c += LANES)
        {
            FloatVector a0i = FloatVector.fromArray(SPECIES, xi, in + c);
            FloatVector a0q = FloatVector.fromArray(SPECIES, xq, in + c);
            FloatVector a1i = FloatVector.fromArray(SPECIES, xi, i1 + c);
            FloatVector a1q = FloatVector.fromArray(SPECIES, xq, i1 + c);
            a0i.add(a1i).intoArray(yi, out + c);
            a0q.add(a1q).intoArray(yq, out + c);
            FloatVector di = a0i.sub(a1i);
            FloatVector dq = a0q.sub(a1q);
            di.mul(w1i).sub(dq.mul(w1q)).intoArray(yi, o1 + c);
            di.mul(w1q).add(dq.mul(w1i)).intoArray(yq, o1 + c);
        }

    }

    /**
     * Radix 3 butterfly of width columns.
     */
    private void radix3(float[] xi, float[] xq, float[] yi, float[] yq, int in, int is, int out, int os, int width, float w1i, float w1q, float w2i, float w2q)
    {
        int i1 = in + is;
        int i2 = i1 + is;
        int o1 = out + os;
        int o2 = o1 + os;
        for(int c = 0; c < width; c += LANES)
        {
            FloatVector a0i = FloatVector.fromArray(SPECIES, xi, in + c);
            FloatVector a0q = FloatVector.fromArray(SPECIES, xq, in + c);
            FloatVector a1i = FloatVector.fromArray(SPECIES, xi, i1 + c);
            FloatVector a1q = FloatVector.fromArray(SPECIES, xq, i1 + c);
            FloatVector a2i = FloatVector.fromArray(SPECIES, xi, i2 + c);
            FloatVector a2q = FloatVector.fromArray(SPECIES, xq, i2 + c);
            FloatVector t1i = a1i.add(a2i);
            FloatVector t1q = a1q.add(a2q);
            FloatVector t2i = a1i.sub(a2i).mul(S3);
            FloatVector t2q = a1q.sub(a2q).mul(S3);
            a0i.add(t1i).intoArray(yi, out + c);
            a0q.add(t1q).intoArray(yq, out + c);
            FloatVector bi = t1i.mul(C3).add(a0i);
            FloatVector bq = t1q.mul(C3).add(a0q);
            FloatVector x1i = bi.add(t2q);
            FloatVector x1q = bq.sub(t2i);
            FloatVector x2i = bi.sub(t2q);
            FloatVector x2q = bq.add(t2i);
            x1i.mul(w1i).sub(x1q.mul(w1q)).intoArray(yi, o1 + c);
            x1i.mul(w1q).add(x1q.mul(w1i)).intoArray(yq, o1 + c);
            x2i.mul(w2i).sub(x2q.mul(w2q)).intoArray(yi, o2 + c);
            x2i.mul(w2q).add(x2q.mul(w2i)).intoArray(yq, o2 + c);
        }

    }

    /**
     * Radix 4 butterfly of width columns, for the first group where every twiddle is one.
     */
    private void radix4(float[] xi, float[] xq, float[] yi, float[] yq, int in, int is, int out, int os, int width)
    {
        int i1 = in + is;
        int i2 = i1 + is;
        int i3 = i2 + is;
        int o1 = out + os;
        int o2 = o1 + os;
        int o3 = o2 + os;
        for(int c = 0; c < width; c += LANES)
        {
            FloatVector a0i = FloatVector.fromArray(SPECIES, xi, in + c);
            FloatVector a0q = FloatVector.fromArray(SPECIES, xq, in + c);
            FloatVector a1i = FloatVector.fromArray(SPECIES, xi, i1 + c);
            FloatVector a1q = FloatVector.fromArray(SPECIES, xq, i1 + c);
            FloatVector a2i = FloatVector.fromArray(SPECIES, xi, i2 + c);
            FloatVector a2q = FloatVector.fromArray(SPECIES, xq, i2 + c);
            FloatVector a3i = FloatVector.fromArray(SPECIES, xi, i3 + c);
            FloatVector a3q = FloatVector.fromArray(SPECIES, xq, i3 + c);
            FloatVector t0i = a0i.add(a2i);
            FloatVector t0q = a0q.add(a2q);
            FloatVector t1i = a0i.sub(a2i);
            FloatVector t1q = a0q.sub(a2q);
            FloatVector t2i = a1i.add(a3i);
            FloatVector t2q = a1q.add(a3q);
            FloatVector t3i = a1q.sub(a3q);
            FloatVector t3q = a3i.sub(a1i);
            t0i.add(t2i).intoArray(yi, out + c);
            t0q.add(t2q).intoArray(yq, out + c);
            t1i.add(t3i).intoArray(yi, o1 + c);
            t1q.add(t3q).intoArray(yq, o1 + c);
            t0i.sub(t2i).intoArray(yi, o2 + c);
            t0q.sub(t2q).intoArray(yq, o2 + c);
            t1i.sub(t3i).intoArray(yi, o3 + c);
            t1q.sub(t3q).intoArray(yq, o3 + c);
        }

    }

    /**
     * Radix 4 butterfly of width columns.
     */
    private void radix4(float[] xi, float[] xq, float[] yi, float[] yq, int in, int is, int out, int os, int width, float w1i, float w1q, float w2i, float w2q, float w3i, float w3q)
    {
        int i1 = in + is;
        int i2 = i1 + is;
        int i3 = i2 + is;
        int o1 = out + os;
        int o2 = o1 + os;
        int o3 = o2 + os;
        for(int c = 0; c < width; c += LANES)
        {
            FloatVector a0i = FloatVector.fromArray(SPECIES, xi, in + c);
            FloatVector a0q = FloatVector.fromArray(SPECIES, xq, in + c);
            FloatVector a1i = FloatVector.fromArray(SPECIES, xi, i1 + c);
            FloatVector a1q = FloatVector.fromArray(SPECIES, xq, i1 + c);
            FloatVector a2i = FloatVector.fromArray(SPECIES, xi, i2 + c);
            FloatVector a2q = FloatVector.fromArray(SPECIES, xq, i2 + c);
            FloatVector a3i = FloatVector.fromArray(SPECIES, xi, i3 + c);
            FloatVector a3q = FloatVector.fromArray(SPECIES, xq, i3 + c);
            FloatVector t0i = a0i.add(a2i);
            FloatVector t0q = a0q.add(a2q);
            FloatVector t1i = a0i.sub(a2i);
            FloatVector t1q = a0q.sub(a2q);
            FloatVector t2i = a1i.add(a3i);
            FloatVector t2q = a1q.add(a3q);
            FloatVector t3i = a1q.sub(a3q);
            FloatVector t3q = a3i.sub(a1i);
            t0i.add(t2i).intoArray(yi, out + c);
            t0q.add(t2q).intoArray(yq, out + c);
            FloatVector x1i = t1i.add(t3i);
            FloatVector x1q = t1q.add(t3q);
            FloatVector x2i = t0i.sub(t2i);
            FloatVector x2q = t0q.sub(t2q);
            FloatVector x3i = t1i.sub(t3i);
            FloatVector x3q = t1q.sub(t3q);
            x1i.mul(w1i).sub(x1q.mul(w1q)).intoArray(yi, o1 + c);
            x1i.mul(w1q).add(x1q.mul(w1i)).intoArray(yq, o1 + c);
            x2i.mul(w2i).sub(x2q.mul(w2q)).intoArray(yi, o2 + c);
            x2i.mul(w2q).add(x2q.mul(w2i)).intoArray(yq, o2 + c);
            x3i.mul(w3i).sub(x3q.mul(w3q)).intoArray(yi, o3 + c);
            x3i.mul(w3q).add(x3q.mul(w3i)).intoArray(yq, o3 + c);
        }

    }

    /**
     * Radix 5 butterfly of width columns.
     */
    private void radix5(float[] xi, float[] xq, float[] yi, float[] yq, int in, int is, int out, int os, int width, float w1i, float w1q, float w2i, float w2q, float w3i, float w3q, float w4i,
                          float w4q)
    {
        int i1 = in + is;
        int i2 = i1 + is;
        int i3 = i2 + is;
        int i4 = i3 + is;
        int o1 = out + os;
        int o2 = o1 + os;
        int o3 = o2 + os;
        int o4 = o3 + os;
        for(int c = 0; c < width; c += LANES)
        {
            FloatVector a0i = FloatVector.fromArray(SPECIES, xi, in + c);
            FloatVector a0q = FloatVector.fromArray(SPECIES, xq, in + c);
            FloatVector a1i = FloatVector.fromArray(SPECIES, xi, i1 + c);
            FloatVector a1q = FloatVector.fromArray(SPECIES, xq, i1 + c);
            FloatVector a2i = FloatVector.fromArray(SPECIES, xi, i2 + c);
            FloatVector a2q = FloatVector.fromArray(SPECIES, xq, i2 + c);
            FloatVector a3i = FloatVector.fromArray(SPECIES, xi, i3 + c);
            FloatVector a3q = FloatVector.fromArray(SPECIES, xq, i3 + c);
            FloatVector a4i = FloatVector.fromArray(SPECIES, xi, i4 + c);
            FloatVector a4q = FloatVector.fromArray(SPECIES, xq, i4 + c);
            FloatVector t1i = a1i.add(a4i);
            FloatVector t1q = a1q.add(a4q);
            FloatVector t2i = a2i.add(a3i);
            FloatVector t2q = a2q.add(a3q);
            FloatVector t3i = a1i.sub(a4i);
            FloatVector t3q = a1q.sub(a4q);
            FloatVector t4i = a2i.sub(a3i);
            FloatVector t4q = a2q.sub(a3q);
            a0i.add(t1i).add(t2i).intoArray(yi, out + c);
            a0q.add(t1q).add(t2q).intoArray(yq, out + c);
            FloatVector b1i = t2i.mul(C52).add(t1i.mul(C51).add(a0i));
            FloatVector b1q = t2q.mul(C52).add(t1q.mul(C51).add(a0q));
            FloatVector b2i = t2i.mul(C51).add(t1i.mul(C52).add(a0i));
            FloatVector b2q = t2q.mul(C51).add(t1q.mul(C52).add(a0q));
            FloatVector d1i = t4i.mul(S52).add(t3i.mul(S51));
            FloatVector d1q = t4q.mul(S52).add(t3q.mul(S51));
            FloatVector d2i = t4i.mul(-S51).add(t3i.mul(S52));
            FloatVector d2q = t4q.mul(-S51).add(t3q.mul(S52));
            FloatVector x1i = b1i.add(d1q);
            FloatVector x1q = b1q.sub(d1i);
            FloatVector x4i = b1i.sub(d1q);
            FloatVector x4q = b1q.add(d1i);
            FloatVector x2i = b2i.add(d2q);
            FloatVector x2q = b2q.sub(d2i);
            FloatVector x3i = b2i.sub(d2q);
            FloatVector x3q = b2q.add(d2i);
            x1i.mul(w1i).sub(x1q.mul(w1q)).intoArray(yi, o1 + c);
            x1i.mul(w1q).add(x1q.mul(w1i)).intoArray(yq, o1 + c);
            x2i.mul(w2i).sub(x2q.mul(w2q)).intoArray(yi, o2 + c);
            x2i.mul(w2q).add(x2q.mul(w2i)).intoArray(yq, o2 + c);
            x3i.mul(w3i).sub(x3q.mul(w3q)).intoArray(yi, o3 + c);
            x3i.mul(w3q).add(x3q.mul(w3i)).intoArray(yq, o3 + c);
            x4i.mul(w4i).sub(x4q.mul(w4q)).intoArray(yi, o4 + c);
            x4i.mul(w4q).add(x4q.mul(w4i)).intoArray(yq, o4 + c);
        }

    }
}
