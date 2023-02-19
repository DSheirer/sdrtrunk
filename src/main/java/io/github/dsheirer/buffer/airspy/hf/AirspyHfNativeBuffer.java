/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.buffer.airspy.hf;

import io.github.dsheirer.buffer.AbstractNativeBuffer;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import java.util.Iterator;

/**
 * Native buffer implementation for Airspy HF+ & Discovery tuners.
 */
public class AirspyHfNativeBuffer extends AbstractNativeBuffer
{
    public static final float SCALE = 1.0f / 32768.0f;
    private short[] mInterleavedSamples;
    private float mAverageDc;

    /**
     * Constructs an instance.
     * @param timestamp of the first sample of the buffer
     * @param interleavedSamples pairs of 16-bit complex samples.
     */
    public AirspyHfNativeBuffer(long timestamp, float samplesPerMillisecond, float averageDc, short[] interleavedSamples)
    {
        super(timestamp, samplesPerMillisecond);
        mInterleavedSamples = interleavedSamples;
        mAverageDc = averageDc;
    }

    @Override
    public Iterator<ComplexSamples> iterator()
    {
        return new IteratorComplexSamples();
    }

    @Override
    public Iterator<InterleavedComplexSamples> iteratorInterleaved()
    {
        return new IteratorInterleaved();
    }

    @Override
    public int sampleCount()
    {
        return mInterleavedSamples.length / 2;
    }

    /**
     * Scalar implementation of complex samples buffer iterator
     */
    public class IteratorComplexSamples implements Iterator<ComplexSamples>
    {
        private boolean mHasNext = true;

        @Override
        public boolean hasNext()
        {
            return mHasNext;
        }

        @Override
        public ComplexSamples next()
        {
            int length = mInterleavedSamples.length / 2;
            float[] i = new float[length];
            float[] q = new float[length];

            int index = 0;

            for(int x = 0; x < mInterleavedSamples.length; x += 2)
            {
                index = x / 2;
                //Native ordering is in Q, I, Q, I ... reverse it to I, Q, I, Q
                i[index] = (mInterleavedSamples[x + 1] * SCALE) - mAverageDc;
                q[index] = (mInterleavedSamples[x] * SCALE) - mAverageDc;
            }

            mHasNext = false;

            return new ComplexSamples(i, q, getTimestamp());
        }
    }

    /**
     * Scalar implementation of interleaved sample buffer iterator.
     */
    public class IteratorInterleaved implements Iterator<InterleavedComplexSamples>
    {
        private boolean mHasNext = true;

        @Override
        public boolean hasNext()
        {
            return mHasNext;
        }

        @Override
        public InterleavedComplexSamples next()
        {
            float[] samples = new float[mInterleavedSamples.length];

            for(int x = 0; x < mInterleavedSamples.length; x += 2)
            {
                //Native ordering is in Q, I, Q, I ... reverse it to I, Q, I, Q
                samples[x] = (mInterleavedSamples[x + 1] * SCALE) - mAverageDc;
                samples[x + 1] = (mInterleavedSamples[x] * SCALE) - mAverageDc;
            }

            mHasNext = false;

            return new InterleavedComplexSamples(samples, getTimestamp());
        }
    }
}
