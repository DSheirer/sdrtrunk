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

import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import java.util.Iterator;

/**
 * Native buffer sample array wrapper class that provides access to a stream of either interleaved or
 * non-interleaved complex sample buffers converted from the raw byte sample array.
 */
public class SignedByteNativeBuffer extends AbstractNativeBuffer
{
    private static final int FRAGMENT_SIZE = 2048;
    private final static float[] LOOKUP_VALUES;

    //Creates a static lookup table that converts the signed byte values as:
    // Index   0 - 127: 0.0f to 1.0f
    // Index 128 - 255: -1.0f to -0.0f
    static
    {
        LOOKUP_VALUES = new float[256];

        for(int x = 0; x < 256; x++)
        {
            LOOKUP_VALUES[x] = (float)((byte)x) / 128.0f;
        }
    }

    private byte[] mSamples;
    private float mIAverageDc;
    private float mQAverageDc;

    /**
     * Constructs an instance
     * @param samples to process
     * @param timestamp of the samples
     * @param iAverageDc of the sample stream
     * @param qAverageDc of the sample stream
     * @param samplesPerMillisecond to calculate sub-buffer timestamps
     */
    public SignedByteNativeBuffer(byte[] samples, long timestamp, float iAverageDc, float qAverageDc, float samplesPerMillisecond)
    {
        super(timestamp, samplesPerMillisecond);

        //Ensure we're an even multiple of the fragment size.  Typically, this will be 64k or 128k
        if(samples.length % FRAGMENT_SIZE != 0)
        {
            throw new IllegalArgumentException("Samples byte[] length [" + samples.length + "] must be an even multiple of " + FRAGMENT_SIZE);
        }

        mSamples = samples;
        mIAverageDc = iAverageDc;
        mQAverageDc = qAverageDc;
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

    /**
     * Iterator of complex samples over the native byte buffer array
     */
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
                i[pointer] = LOOKUP_VALUES[(0xFF & mSamples[samplesOffset++])] - mIAverageDc;
                q[pointer] = LOOKUP_VALUES[(0xFF & mSamples[samplesOffset++])] - mQAverageDc;
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

            int samplesOffset = mSamplesPointer;

            for(int pointer = 0; pointer < converted.length; pointer += 2)
            {
                converted[pointer] = LOOKUP_VALUES[(0xFF & mSamples[samplesOffset++])] - mIAverageDc;
                converted[pointer + 1] = LOOKUP_VALUES[(0xFF & mSamples[samplesOffset++])] - mQAverageDc;
            }

            mSamplesPointer = samplesOffset;

            return new InterleavedComplexSamples(converted, timestamp);
        }
    }
}
