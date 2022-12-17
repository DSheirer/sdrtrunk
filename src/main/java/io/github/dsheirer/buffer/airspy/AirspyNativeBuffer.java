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

package io.github.dsheirer.buffer.airspy;

import io.github.dsheirer.buffer.AbstractNativeBuffer;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import io.github.dsheirer.vector.calibrate.Implementation;
import java.util.Iterator;

/**
 * Native buffer scalar implementation for Airspy non-packed samples.
 */
public class AirspyNativeBuffer extends AbstractNativeBuffer
{
    private short[] mSamples;
    private short[] mResidualI;
    private short[] mResidualQ;
    private float mAverageDc;
    private Implementation mInterleavedImplementation;
    private Implementation mNonInterleavedImplementation;

    /**
     * Constructs an instance
     * @param samples (non-packed) from the airspy device
     * @param residualI samples from previous buffer
     * @param residualQ samples from previous buffer
     * @param averageDc measured
     * @param timestamp of the buffer
     * @param interleavedImplementation optimal, scalar vs vector SIMD
     * @param nonInterleavedImplementation optimal, scalar vs vector SIMD
     * @param samplesPerMillisecond used to calculate sub-buffer fragment timestamp offsets from the start of this buffer.
     */
    public AirspyNativeBuffer(short[] samples, short[] residualI, short[] residualQ, float averageDc,
                              long timestamp, Implementation interleavedImplementation,
                              Implementation nonInterleavedImplementation, float samplesPerMillisecond)
    {
        super(timestamp, samplesPerMillisecond);

        //Ensure we're an even multiple of the fragment size.  Typically, this will be 64k or 128k
        if(samples.length % AirspyBufferIterator.FRAGMENT_SIZE != 0)
        {
            throw new IllegalArgumentException("Samples short[] length [" + samples.length +
                    "] must be an even multiple of " + AirspyBufferIterator.FRAGMENT_SIZE);
        }

        mSamples = samples;
        mResidualI = residualI;
        mResidualQ = residualQ;
        mAverageDc = averageDc;
        mInterleavedImplementation = interleavedImplementation;
        mNonInterleavedImplementation = nonInterleavedImplementation;
    }

    @Override
    public int sampleCount()
    {
        return mSamples.length / 2;
    }

    @Override
    public Iterator<ComplexSamples> iterator()
    {
        return switch(mInterleavedImplementation)
        {
            case VECTOR_SIMD_512 -> new AirspyBufferIteratorVector512Bits(mSamples, mResidualI, mResidualQ, mAverageDc, getTimestamp(), getSamplesPerMillisecond());
            case VECTOR_SIMD_256-> new AirspyBufferIteratorVector256Bits(mSamples, mResidualI, mResidualQ, mAverageDc, getTimestamp(), getSamplesPerMillisecond());
            case VECTOR_SIMD_128-> new AirspyBufferIteratorVector128Bits(mSamples, mResidualI, mResidualQ, mAverageDc, getTimestamp(), getSamplesPerMillisecond());
            case VECTOR_SIMD_64 ->new AirspyBufferIteratorVector64Bits(mSamples, mResidualI, mResidualQ, mAverageDc, getTimestamp(), getSamplesPerMillisecond());
            default -> new AirspyBufferIteratorScalar(mSamples, mResidualI, mResidualQ, mAverageDc, getTimestamp(), getSamplesPerMillisecond());
        };
    }

    @Override
    public Iterator<InterleavedComplexSamples> iteratorInterleaved()
    {
        return switch(mInterleavedImplementation)
        {
            case VECTOR_SIMD_512 -> new AirspyInterleavedBufferIteratorVector512Bits(mSamples, mResidualI, mResidualQ, mAverageDc, getTimestamp(), getSamplesPerMillisecond());
            case VECTOR_SIMD_256-> new AirspyInterleavedBufferIteratorVector256Bits(mSamples, mResidualI, mResidualQ, mAverageDc, getTimestamp(), getSamplesPerMillisecond());
            case VECTOR_SIMD_128-> new AirspyInterleavedBufferIteratorVector128Bits(mSamples, mResidualI, mResidualQ, mAverageDc, getTimestamp(), getSamplesPerMillisecond());
            case VECTOR_SIMD_64 ->new AirspyInterleavedBufferIteratorVector64Bits(mSamples, mResidualI, mResidualQ, mAverageDc, getTimestamp(), getSamplesPerMillisecond());
            default -> new AirspyInterleavedBufferIteratorScalar(mSamples, mResidualI, mResidualQ, mAverageDc, getTimestamp(), getSamplesPerMillisecond());
        };
    }
}