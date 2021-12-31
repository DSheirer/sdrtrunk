/*
 * *****************************************************************************
 * Copyright (C) 2014-2021 Dennis Sheirer
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
package io.github.dsheirer.dsp.filter.halfband.real;

import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;

/**
 * Implements a half-band filter that produces one filtered output for every two input samples.
 *
 * This filter is structured for Java 8+ compiler JIT optimization for SIMD instructions when
 * supported by the host CPU.
 *
 * Note: this class is structured to process an entire float array, versus processing one sample at a time from the
 * array.
 */
public class RealHalfBandDecimationFilter implements IRealDecimationFilter
{
    private static final float CENTER_COEFFICIENT = 0.5f;
    private float[] mCoefficients;
    private float[] mBuffer;
    private float mAccumulator;
    private int mCoefficientPointer;
    private int mCoefficientsLengthMinus1;
    private int mBufferPointer;
    private int mHalf;

    /**
     * Creates a half band filter with inherent decimation by two.
     *
     * @param coefficients of the half-band filter that is odd-length where all odd index coefficients are
     * zero valued except for the middle odd index coefficient which should be valued 0.5
     */
    public RealHalfBandDecimationFilter(float[] coefficients)
    {
        if((coefficients.length + 1) % 4 != 0)
        {
            throw new IllegalArgumentException("Half-band filter coefficients must be odd-length and symmetrical (length = [x * 4 + 1]");
        }

        mCoefficients = coefficients;
        mCoefficientsLengthMinus1 = mCoefficients.length - 1;
        mHalf = mCoefficientsLengthMinus1 / 2;
    }

    public float[] decimateReal(float[] samples)
    {
        if(samples.length % 2 != 0)
        {
            throw new IllegalArgumentException("Samples array length must be an integer multiple of 2");
        }

        int bufferLength = samples.length + mCoefficientsLengthMinus1;

        if(mBuffer == null)
        {
            mBuffer = new float[bufferLength];
        }
        else if(mBuffer.length != bufferLength)
        {
            float[] temp = new float[bufferLength];
            //Move residual samples from end of old buffer to the beginning of the new temp buffer
            System.arraycopy(mBuffer, mBuffer.length - mCoefficientsLengthMinus1, temp, 0, mCoefficientsLengthMinus1);
            mBuffer = temp;
        }
        else
        {
            //Move residual samples from end of buffer to the beginning of the buffer
            System.arraycopy(mBuffer, samples.length, mBuffer, 0, mCoefficientsLengthMinus1);
        }

        //Copy new sample array into end of buffer
        System.arraycopy(samples, 0, mBuffer, mCoefficientsLengthMinus1, samples.length);

        float[] filtered = new float[samples.length / 2];

        for(mBufferPointer = 0; mBufferPointer < samples.length; mBufferPointer += 2)
        {
            mAccumulator = 0.0f;

            for(mCoefficientPointer = 0; mCoefficientPointer < mHalf; mCoefficientPointer += 2)
            {
                //Half band filter coefficients are mirrored, so we add the mirrored samples and then multiply by
                //one of the coefficients to achieve the same effect.
                mAccumulator += mCoefficients[mCoefficientPointer] *
                        (mBuffer[mBufferPointer + mCoefficientPointer] +
                                mBuffer[mBufferPointer + (mCoefficientsLengthMinus1 - mCoefficientPointer)]);
            }

            mAccumulator += mBuffer[mBufferPointer + mHalf] * CENTER_COEFFICIENT;

            filtered[mBufferPointer / 2] = mAccumulator;
        }

        return filtered;
    }
}
