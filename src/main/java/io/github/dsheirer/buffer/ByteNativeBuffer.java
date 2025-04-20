/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import java.util.Iterator;

/**
 * Native buffer sample array wrapper class that provides access to a stream of either interleaved or
 * non-interleaved complex sample buffers converted from the raw byte sample array.
 */
public class ByteNativeBuffer extends AbstractNativeBuffer
{
    private static final int FRAGMENT_SIZE = 8192;
    private final static float[] LOOKUP_VALUES;
    private float mAverageDc;

    //Creates a static lookup table that converts the 8-bit valued range from 0 - 255 into scaled float values
    //of -1.0 to 0 to 1.0
    static
    {
        LOOKUP_VALUES = new float[256];

        for(int x = 0; x < 256; x++)
        {
            LOOKUP_VALUES[x] = ((float)x - 127.5f) / 128.0f;
        }
    }

    private byte[] mSamples;

    /**
     * Constructs an instance
     * @param samples to process
     * @param timestamp of the samples
     * @param averageDc measured from sample stream
     * @param samplesPerMillisecond to calculate derivative timestamps for sub-buffers.
     */
    public ByteNativeBuffer(byte[] samples, long timestamp, float averageDc, float samplesPerMillisecond)
    {
        super(timestamp, samplesPerMillisecond);
        //Ensure we're an even multiple of the fragment size.  Typically, this will be 64k or 128k
        if(samples.length % FRAGMENT_SIZE != 0)
        {
            throw new IllegalArgumentException("Samples byte[] length [" + samples.length + "] must be an even multiple of " + FRAGMENT_SIZE);
        }

        mSamples = samples;
        mAverageDc = averageDc;
    }

    @Override
    public int sampleCount()
    {
        return mSamples.length / 2;
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

    private class ComplexSamplesIterator implements Iterator<ComplexSamples>
    {
        private int mSamplesPointer = 0;

        @Override
        public boolean hasNext()
        {
            return mSamplesPointer < mSamples.length;
        }

        @Override
        public ComplexSamples next()
        {
            long timestamp = getFragmentTimestamp(mSamplesPointer);

            float[] i = new float[FRAGMENT_SIZE];
            float[] q = new float[FRAGMENT_SIZE];
            int samplesOffset = mSamplesPointer;

            for(int pointer = 0; pointer < i.length; pointer++)
            {
                i[pointer] = LOOKUP_VALUES[(0xFF & mSamples[samplesOffset++])] - mAverageDc;
                q[pointer] = LOOKUP_VALUES[(0xFF & mSamples[samplesOffset++])] - mAverageDc;
            }

            mSamplesPointer = samplesOffset;
            return new ComplexSamples(i, q, timestamp);
        }
    }

    /**
     * Iterator of interleaved complex samples over the native byte buffer array.
     */
    private class InterleavedComplexSamplesIterator implements Iterator<InterleavedComplexSamples>
    {
        private int mSamplesPointer = 0;

        @Override
        public boolean hasNext()
        {
            return mSamplesPointer < mSamples.length;
        }

        @Override
        public InterleavedComplexSamples next()
        {
            long timestamp = getFragmentTimestamp(mSamplesPointer);

            float[] converted = new float[FRAGMENT_SIZE * 2];

            int samplesPointer = mSamplesPointer;

            for(int pointer = 0; pointer < converted.length; pointer++)
            {
                converted[pointer] = LOOKUP_VALUES[(0xFF & mSamples[samplesPointer++])] - mAverageDc;
            }

            mSamplesPointer = samplesPointer;

            return new InterleavedComplexSamples(converted, timestamp);
        }
    }
}
