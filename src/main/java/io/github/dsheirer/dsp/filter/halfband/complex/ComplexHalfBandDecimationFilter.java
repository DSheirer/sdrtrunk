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

package io.github.dsheirer.dsp.filter.halfband.complex;

import io.github.dsheirer.dsp.filter.decimate.IComplexDecimationFilter;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBufferQueue;

/**
 * Complex half-band filter that processes samples on a per-array basis, versus a per-sample basis.
 */
public class ComplexHalfBandDecimationFilter implements IComplexDecimationFilter
{
    private static final float CENTER_COEFFICIENT = 0.5f;
    private float[] mCoefficients;
    private float[] mBuffer;
    private float mIAccumulator;
    private float mQAccumulator;
    private int mCoefficientPointer;
    private int mCoefficientsLengthMinus2;
    private int mBufferPointer;
    private int mHalf;
    private ReusableComplexBufferQueue mReusableComplexBufferQueue = new ReusableComplexBufferQueue("half-band decimation filter");

    /**
     * Constructs a complex sample half-band decimate x2 filter using the specified filter coefficients.
     * @param coefficients for the half-band filter where the middle coefficient is 0.5, and the even-numbered
     * coefficients are non-zero and symmetrical, and the odd-numbered coefficients are all zero valued.
     */
    public ComplexHalfBandDecimationFilter(float[] coefficients)
    {
        if((coefficients.length + 1) % 4 != 0)
        {
            throw new IllegalArgumentException("Half-band filter coefficients must be odd-length and " +
                    "symmetrical where L = [x * 4 - 1]");
        }

        mCoefficients = new float[coefficients.length * 2];
        for(int x = 0; x < coefficients.length; x++)
        {
            mCoefficients[2 * x] = coefficients[x];
            mCoefficients[2 * x + 1] = coefficients[x];
        }

        mCoefficientsLengthMinus2 = mCoefficients.length - 2;
        mHalf = mCoefficients.length / 2 - 1;
    }

    public float[] decimateComplex(float[] samples)
    {
        if(samples.length % 4 != 0)
        {
            throw new IllegalArgumentException("Samples array length must be an integer multiple of 4");
        }

        int bufferLength = samples.length + mCoefficientsLengthMinus2;

        if(mBuffer == null)
        {
            mBuffer = new float[bufferLength];
        }
        else if(mBuffer.length != bufferLength)
        {
            float[] temp = new float[bufferLength];
            //Move residual samples from end of old buffer to the beginning of the new temp buffer
            System.arraycopy(mBuffer, mBuffer.length - mCoefficientsLengthMinus2, temp, 0, mCoefficientsLengthMinus2);
            mBuffer = temp;
        }
        else
        {
            //Move residual samples from end of buffer to the beginning of the buffer
            System.arraycopy(mBuffer, samples.length, mBuffer, 0, mCoefficientsLengthMinus2);
        }

        //Copy new sample array into end of buffer
        System.arraycopy(samples, 0, mBuffer, mCoefficientsLengthMinus2, samples.length);

        float[] filtered = new float[samples.length / 2];

        for(mBufferPointer = 0; mBufferPointer < samples.length; mBufferPointer += 4)
        {
            mIAccumulator = 0.0f;
            mQAccumulator = 0.0f;

            for(mCoefficientPointer = 0; mCoefficientPointer < mHalf; mCoefficientPointer += 4)
            {
                //Half band filter coefficients are mirrored, so we add the mirrored samples and then multiply by
                //one of the coefficients to achieve the same effect.
                mIAccumulator += mCoefficients[mCoefficientPointer] *
                        (mBuffer[mBufferPointer + mCoefficientPointer] +
                                mBuffer[mBufferPointer + (mCoefficientsLengthMinus2 - mCoefficientPointer)]);

                mQAccumulator += mCoefficients[mCoefficientPointer] *
                        (mBuffer[mBufferPointer + mCoefficientPointer + 1] +
                                mBuffer[mBufferPointer + (mCoefficientsLengthMinus2 - mCoefficientPointer) + 1]);
            }

            mIAccumulator += mBuffer[mBufferPointer + mHalf] * CENTER_COEFFICIENT;
            mQAccumulator += mBuffer[mBufferPointer + mHalf + 1] * CENTER_COEFFICIENT;

            filtered[mBufferPointer / 2] = mIAccumulator;
            filtered[mBufferPointer / 2 + 1] = mQAccumulator;
        }

        return filtered;
    }

    /**
     * Decimates the complex samples and returns a buffer of decimated samples.
     * @param buffer to decimate
     * @return decimated buffer.
     */
    @Override
    public ReusableComplexBuffer decimate(ReusableComplexBuffer buffer)
    {
        float[] decimated = decimateComplex(buffer.getSamples());
        buffer.decrementUserCount();
        return mReusableComplexBufferQueue.getBuffer(decimated, buffer.getTimestamp());
    }
}
