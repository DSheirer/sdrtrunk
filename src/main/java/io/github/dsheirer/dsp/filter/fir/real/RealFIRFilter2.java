/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.dsp.filter.fir.real;

import io.github.dsheirer.dsp.filter.fir.FIRFilter;
import io.github.dsheirer.sample.buffer.ReusableBuffer;
import io.github.dsheirer.sample.buffer.ReusableBufferQueue;

/**
 * Finite Impulse Response (FIR) filter for filtering individual float samples or float sample arrays.
 *
 * Note: filtering operations in this class are structured to leverage SIMD processor intrinsics when
 * available to the Java runtime.
 */
public class RealFIRFilter2 extends FIRFilter
{
    private ReusableBufferQueue mReusableBufferQueue = new ReusableBufferQueue();

    private float[] mData;
    private float[] mCoefficients;
    private float mGain;
    private float mAccumulator;

    /**
     * Float sample FIR filter base class.
     *
     * @param coefficients - filter coefficients in normal order.
     * @param gain value to apply to the filtered output.  Use 1.0f for no gain
     */
    public RealFIRFilter2(float[] coefficients, float gain)
    {
        mGain = gain;
        mCoefficients = coefficients;
        mData = new float[coefficients.length];
    }

    /**
     * Float sample FIR filter base class that uses a default gain value of 1.0f.
     *
     * @param coefficients - filter coefficients in normal order.
     */
    public RealFIRFilter2(float[] coefficients)
    {
        this(coefficients, 1.0f);
    }

    /**
     * Disposes this filter to prepare for garbage collection.
     */
    @Override
    public void dispose()
    {
        mCoefficients = null;
        mData = null;
    }

    /**
     * Filters the sample argument.  Loads the sample into the internal data buffer and performs
     * convolution between the sample buffer and the filter coefficients.
     *
     * @param sample to load
     * @return filtered value
     */
    public float filter(float sample)
    {
        //Use array copy to leverage SIMD intrinsics
        System.arraycopy(mData, 0, mData, 1, mData.length - 1);
        mData[0] = sample;

        mAccumulator = 0.0f;

        //Use vector dot product to leverage SIMD intrinsics
        for(int x = 0; x < mCoefficients.length; x++)
        {
            mAccumulator += mData[x] * mCoefficients[x];
        }

        /* Apply gain and return the filtered value */
        mAccumulator *= mGain;

        return mAccumulator;
    }

    /**
     * Current filtered output value for the filter after the filter() method has been invoked.
     */
    public float currentValue()
    {
        return mAccumulator;
    }


    /**
     * Filters the samples contained in the unfilteredBuffer and returns a new reusable buffer with the
     * filtered samples.
     *
     * Note: user count on the returned (new) buffer is set to one and the user count is decremented on
     * the unfiltered buffer argument.
     *
     * @param unfilteredBuffer containing a sample array to be filtered
     * @return a new reusable buffer with the filtered samples.
     */
    public ReusableBuffer filter(ReusableBuffer unfilteredBuffer)
    {
        float[] unfilteredSamples = unfilteredBuffer.getSamples();

        ReusableBuffer filteredBuffer = mReusableBufferQueue.getBuffer(unfilteredSamples.length);
        float[] filteredSamples = filteredBuffer.getSamples();

        for(int x = 0; x < unfilteredSamples.length; x++)
        {
            filteredSamples[x] = filter(unfilteredSamples[x]);
        }

        filteredBuffer.incrementUserCount();
        unfilteredBuffer.decrementUserCount();

        return filteredBuffer;
    }
}