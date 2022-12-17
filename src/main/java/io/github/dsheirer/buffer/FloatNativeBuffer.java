/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.buffer;

import io.github.dsheirer.sample.SampleUtils;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import java.util.Iterator;

/**
 * Simple native buffer implementation that simply wraps a single, existing, non-native sample buffer.
 */
public class FloatNativeBuffer extends AbstractNativeBuffer
{
    private float[] mInterleavedComplexSamples;

    /**
     * Constructs an instance
     * @param complexSamples interleaved to wrap
     * @param timestamp for the buffer
     */
    public FloatNativeBuffer(float[] complexSamples, long timestamp, float samplesPerMillisecond)
    {
        super(timestamp, samplesPerMillisecond);
        mInterleavedComplexSamples = complexSamples;
    }

    /**
     * Constructs an instance
     * @param samples to wrap
     * @param timestamp for the buffer
     */
    public FloatNativeBuffer(ComplexSamples samples)
    {
        this(SampleUtils.interleave(samples), samples.timestamp(), 0.0f);
    }

    /**
     * Constructs an instance
     * @param samples to wrap
     */
    public FloatNativeBuffer(InterleavedComplexSamples samples)
    {
        this(samples.samples(), samples.timestamp(), 0.0f);
    }

    @Override
    public Iterator<ComplexSamples> iterator()
    {
        return new ComplexSamplesIterator();
    }

    @Override
    public Iterator<InterleavedComplexSamples> iteratorInterleaved()
    {
        return new InterleavedComplexSamplesIterator();
    }

    @Override
    public int sampleCount()
    {
        return mInterleavedComplexSamples.length / 2;
    }

    private class ComplexSamplesIterator implements  Iterator<ComplexSamples>
    {
        private boolean mEmpty;

        @Override
        public boolean hasNext()
        {
            return !mEmpty;
        }

        @Override
        public ComplexSamples next()
        {
            if(mEmpty)
            {
                throw new IllegalStateException("No more samples");
            }

            mEmpty = true;
            return SampleUtils.deinterleave(mInterleavedComplexSamples, getTimestamp());
        }
    }

    private class InterleavedComplexSamplesIterator implements Iterator<InterleavedComplexSamples>
    {
        private boolean mEmpty;


        @Override
        public boolean hasNext()
        {
            return !mEmpty;
        }

        @Override
        public InterleavedComplexSamples next()
        {
            if(mEmpty)
            {
                throw new IllegalStateException("No more samples");
            }

            mEmpty = true;
            return new InterleavedComplexSamples(mInterleavedComplexSamples, getTimestamp());
        }
    }
}
