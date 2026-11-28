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

/**
 * Vector (SIMD) complex FFT, using butterflies that run across the columns of a matrix (ColumnFFT).
 *
 * A batch of small transforms (below MINIMUM_UNBATCHED_SIZE) is computed a strip of blocks at a time: the strip's
 * blocks are copied side by side as the columns of a matrix, so each butterfly runs across the blocks, with no twiddle
 * or transpose steps between stages.  Other sizes transform a batch one block at a time.
 *
 * A single transform is computed as a matrix (four-step) FFT.  With N = rows x columns, input sample
 * n = columns * n1 + n2 is element (n1, n2) of a row-major matrix.  Step one transforms each column (length rows) a
 * strip of columns at a time, multiplies element (k1, n2) by exp(-j2pi n2 k1 / N) and writes the strip transposed to
 * a work matrix.  Step two transforms each column of the work matrix (length columns) a strip at a time.  Each strip
 * fits in cache and every butterfly runs across a contiguous row of a strip.  The result is in natural order: bin
 * k1 + rows * k2 lands at index k2 * rows + k1.  Below MINIMUM_FOUR_STEP_SIZE, the steps cost more than they save,
 * so single transforms use the mixed radix FFT.
 *
 * The inverse is the forward transform with the inphase and quadrature parts swapped on the way in and out.
 */
public class VectorComplexFFT extends ComplexFFT
{
    /**
     * Smallest size where a single four-step transform is faster than the mixed radix FFT.
     */
    static final int MINIMUM_FOUR_STEP_SIZE = 320;
    /**
     * Smallest size that is transformed one block at a time in a batch.  From here the four-step transform of each
     * block is faster than the batch transform (measured: the batch is faster at 500 and slower at 512).
     */
    static final int MINIMUM_UNBATCHED_SIZE = 512;
    /**
     * Target number of elements in a strip of the four-step transform.  A wider strip makes fewer passes over the
     * rows of the full matrix, and a narrower one keeps the strip in the cache.  The target is not a power of two,
     * so that a strip of power of two length is not a power of two wide: its rows would then be a power of two
     * apart in memory, which is slower (measured).
     */
    private static final int STRIP_ELEMENTS = 50000;
    private static final int LANES = FloatVector.SPECIES_PREFERRED.length();
    /**
     * Target number of elements in a strip of a batch, and the fewest and most blocks in a strip.  The block counts
     * are a whole number of vectors so that the vector loops have no scalar tail.
     */
    private static final int BATCH_ELEMENTS = 16384;
    private static final int BATCH_MINIMUM_WIDTH = Math.max(8, LANES);
    private static final int BATCH_MAXIMUM_WIDTH = 64;

    /**
     * Fewest float lanes in the CPU's preferred vector for the vector FFT to be used.  With 2 lanes (64-bit vectors,
     * as on 32-bit ARM) it has little to gain over the mixed radix FFT, and the 32-bit ARM JVM (JDK 27) crashes or
     * miscalculates in the compiled butterflies.
     */
    private static final int MINIMUM_LANES = 4;

    private final float mScale;
    private final MixedRadixComplexFFT mMixedRadix;
    private final FourStep mFourStep;
    private final Batch mBatch;

    /**
     * Constructs an instance
     * @param size of the transform in complex samples, with no prime factors other than 2, 3 and 5
     * @throws IllegalArgumentException if the size is not supported
     */
    public VectorComplexFFT(int size)
    {
        this(size, size >= MINIMUM_FOUR_STEP_SIZE);
    }

    /**
     * Constructs an instance
     * @param size of the transform in complex samples, with no prime factors other than 2, 3 and 5
     * @param fourStep true to use the four-step transform for single transforms of any size that it supports, or
     * false to use the mixed radix FFT
     */
    VectorComplexFFT(int size, boolean fourStep)
    {
        super(size);
        fourStep &= FourStep.isSupported(size);
        mScale = 1.0f / size;
        mMixedRadix = fourStep ? null : new MixedRadixComplexFFT(size);
        mFourStep = fourStep ? new FourStep(size) : null;
        mBatch = isBatched(size) ? new Batch(size) : null;
    }

    /**
     * Indicates if this CPU's preferred vector is wide enough for the vector FFT.  The factory and the calibration use
     * the scalar FFT when it is not.
     */
    public static boolean isVectorSupported()
    {
        return LANES >= MINIMUM_LANES;
    }

    /**
     * Indicates if the size is transformed as a batch by the batch methods.
     */
    static boolean isBatched(int size)
    {
        return size > 1 && size < MINIMUM_UNBATCHED_SIZE;
    }

    @Override
    public void forward(float[] samples, int offset)
    {
        if(mFourStep != null)
        {
            mFourStep.transform(samples, offset, false, 1.0f);
        }
        else
        {
            mMixedRadix.forward(samples, offset);
        }
    }

    @Override
    public void forward(float[] i, float[] q)
    {
        if(mFourStep != null)
        {
            mFourStep.transform(i, q);
        }
        else
        {
            super.forward(i, q);
        }
    }

    @Override
    public void inverse(float[] samples, int offset, boolean scale)
    {
        if(mFourStep != null)
        {
            mFourStep.transform(samples, offset, true, scale ? mScale : 1.0f);
        }
        else
        {
            mMixedRadix.inverse(samples, offset, scale);
        }
    }

    @Override
    public void forward(float[] samples, int offset, int count)
    {
        if(mBatch != null)
        {
            mBatch.transform(samples, offset, count, false, 1.0f);
        }
        else
        {
            super.forward(samples, offset, count);
        }
    }

    @Override
    public void inverse(float[] samples, int offset, int count, boolean scale)
    {
        if(mBatch != null)
        {
            mBatch.transform(samples, offset, count, true, scale ? mScale : 1.0f);
        }
        else
        {
            super.inverse(samples, offset, count, scale);
        }
    }

    /**
     * Rounds a width up to a whole number of vectors.
     */
    private static int pad(int width)
    {
        return (width + LANES - 1) / LANES * LANES;
    }

    /**
     * Four-step transform of a single block.
     *
     * Every strip is a whole number of vectors wide, so that the vector loops have no scalar tail.  The last strip of
     * a dimension that is not a whole number of vectors ends at the last column and repeats some of the columns of
     * the strip before it, which calculates the same values again.
     */
    private static class FourStep
    {
        private final int mSize;
        private final int mRows;
        private final int mColumns;
        private final int mStrip1;
        private final int mStrip2;
        private final ColumnFFT mStep1;
        private final ColumnFFT mStep2;
        private final float[] mTwiddleI;
        private final float[] mTwiddleQ;
        private final float[] mI;
        private final float[] mQ;
        private final float[] mWorkI;
        private final float[] mWorkQ;
        private final float[] mStripI;
        private final float[] mStripQ;
        private final float[][] mStripWork;

        FourStep(int size)
        {
            mSize = size;
            mRows = getRows(size);
            mColumns = size / mRows;
            mStrip1 = getStripWidth(mColumns, mRows);
            mStrip2 = getStripWidth(mRows, mColumns);
            mStep1 = new ColumnFFT(mRows);
            mStep2 = new ColumnFFT(mColumns);

            //Twiddles stored in input matrix order (k1, n2) so that a strip reads them contiguously
            mTwiddleI = new float[size];
            mTwiddleQ = new float[size];

            for(int k1 = 0; k1 < mRows; k1++)
            {
                for(int n2 = 0; n2 < mColumns; n2++)
                {
                    double angle = -2.0 * Math.PI * ((long)n2 * k1 % size) / size;
                    mTwiddleI[k1 * mColumns + n2] = (float)Math.cos(angle);
                    mTwiddleQ[k1 * mColumns + n2] = (float)Math.sin(angle);
                }
            }

            mI = new float[size];
            mQ = new float[size];
            mWorkI = new float[size];
            mWorkQ = new float[size];
            int strip = Math.max(mRows * mStrip1, mColumns * mStrip2);
            mStripI = new float[strip];
            mStripQ = new float[strip];
            mStripWork = new float[][]{new float[strip], new float[strip], new float[strip], new float[strip]};
        }

        /**
         * Indicates if both dimensions of the matrix are at least one vector, which a strip of whole vectors needs.
         */
        static boolean isSupported(int size)
        {
            int rows = getRows(size);
            return Math.min(rows, size / rows) >= LANES;
        }

        /**
         * Chooses the divisor of size that is nearest to the square root of size, so that both column lengths are
         * similar.
         */
        static int getRows(int size)
        {
            int best = 1;
            double target = Math.sqrt(size);

            for(int rows = 1; rows <= size; rows++)
            {
                if(size % rows == 0 && Math.abs(rows - target) < Math.abs(best - target))
                {
                    best = rows;
                }
            }

            return best;
        }

        /**
         * Chooses the widest strip of whole vectors that fits the dimension and the target number of elements.
         * @param dimension number of columns to divide into strips
         * @param length of each column
         */
        static int getStripWidth(int dimension, int length)
        {
            int width = Math.min(dimension, Math.max(LANES, STRIP_ELEMENTS / length));
            return width - width % LANES;
        }

        /**
         * Transforms interleaved samples, in place.
         * @param samples interleaved complex
         * @param offset to the first sample
         * @param inverse true to swap the inphase and quadrature parts on the way in and out
         * @param gain applied to the results
         */
        void transform(float[] samples, int offset, boolean inverse, float gain)
        {
            final int size = mSize;
            final float[] i = inverse ? mQ : mI;
            final float[] q = inverse ? mI : mQ;

            VectorMoves.deinterleave(samples, offset, i, q, 0, size);
            transform(mI, mQ);
            VectorMoves.interleave(i, q, 0, gain, samples, offset, size);
        }

        /**
         * Forward transform of split samples, in place.
         */
        void transform(float[] i, float[] q)
        {
            final int rows = mRows;
            final int columns = mColumns;
            final float[] stripI = mStripI;
            final float[] stripQ = mStripQ;
            final float[] workI = mWorkI;
            final float[] workQ = mWorkQ;

            //Step 1: transform the columns of the input matrix a strip at a time, twiddle, and write transposed
            for(int start = 0; start < columns; start += mStrip1)
            {
                int strip = Math.min(mStrip1, pad(columns - start));
                int c0 = Math.min(start, columns - strip);
                mStep1.transform(i, q, c0, columns, stripI, stripQ, 0, strip, strip, mStripWork);

                //Row zero's twiddles are all one
                for(int k1 = 1; k1 < rows; k1++)
                {
                    mStep1.multiply(stripI, stripQ, k1 * strip, mTwiddleI, mTwiddleQ, k1 * columns + c0, strip);
                }

                VectorMoves.transpose(stripI, 0, strip, workI, c0 * rows, rows, rows, strip);
                VectorMoves.transpose(stripQ, 0, strip, workQ, c0 * rows, rows, rows, strip);
            }

            //Step 2: transform the columns of the work matrix a strip at a time, into the caller's arrays
            for(int start = 0; start < rows; start += mStrip2)
            {
                int strip = Math.min(mStrip2, pad(rows - start));
                int r0 = Math.min(start, rows - strip);
                mStep2.transform(workI, workQ, r0, rows, i, q, r0, rows, strip, mStripWork);
            }
        }
    }

    /**
     * Batch transform of small blocks, a strip of blocks at a time.
     */
    private static class Batch
    {
        private final int mSize;
        private final int mWidth;
        private final ColumnFFT mFFT;
        private final float[] mI;
        private final float[] mQ;
        private final float[][] mWork;

        Batch(int size)
        {
            mSize = size;
            mWidth = Math.clamp(BATCH_ELEMENTS / size / BATCH_MINIMUM_WIDTH * BATCH_MINIMUM_WIDTH,
                BATCH_MINIMUM_WIDTH, BATCH_MAXIMUM_WIDTH);
            mFFT = new ColumnFFT(size);
            int length = size * mWidth;
            mI = new float[length];
            mQ = new float[length];
            mWork = new float[][]{new float[length], new float[length], new float[length], new float[length]};
        }

        /**
         * Transforms a batch of blocks of interleaved samples, in place.
         * @param samples interleaved complex
         * @param offset to the first sample of the first block
         * @param count of blocks
         * @param inverse true to swap the inphase and quadrature parts on the way in and out
         * @param gain applied to the results
         */
        void transform(float[] samples, int offset, int count, boolean inverse, float gain)
        {
            final int size = mSize;
            final int blockLength = 2 * size;
            final float[] i = inverse ? mQ : mI;
            final float[] q = inverse ? mI : mQ;

            for(int first = 0; first < count; first += mWidth)
            {
                final int blocks = Math.min(mWidth, count - first);
                //The last strip is padded to a whole number of vectors with columns of zeros
                final int width = pad(blocks);
                final int start = offset + first * blockLength;

                //Copy sample n of block b to row n, column b
                for(int n = 0; n < size; n++)
                {
                    int source = start + 2 * n;
                    int row = n * width;

                    for(int b = 0; b < blocks; b++)
                    {
                        i[row + b] = samples[source + b * blockLength];
                        q[row + b] = samples[source + b * blockLength + 1];
                    }

                    for(int b = blocks; b < width; b++)
                    {
                        i[row + b] = 0.0f;
                        q[row + b] = 0.0f;
                    }
                }

                mFFT.transform(mI, mQ, width, mWork);

                for(int n = 0; n < size; n++)
                {
                    int destination = start + 2 * n;
                    int row = n * width;

                    for(int b = 0; b < blocks; b++)
                    {
                        samples[destination + b * blockLength] = i[row + b] * gain;
                        samples[destination + b * blockLength + 1] = q[row + b] * gain;
                    }
                }
            }
        }
    }
}
