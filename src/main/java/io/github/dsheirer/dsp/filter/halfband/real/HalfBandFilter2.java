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
package io.github.dsheirer.dsp.filter.halfband.real;

/**
 * Implements a half-band filter that produces one filtered output for every two input samples.
 *
 * This filter is structured for Java 8+ compiler JIT optimization for SIMD instructions when
 * supported by the host CPU.
 */
public class HalfBandFilter2
{
    private static final float CENTER_COEFFICIENT = 0.5f;
    private float[] mCoefficients;
    private float[] mEvenSamples;
    private float[] mOddSamples;
    private int mCenterSampleIndex;
    private float mAccumulator;
    private int mPointer;
    private float mGain;

    /**
     * Creates a half band filter with inherent decimation by two.
     *
     * @param coefficients of the half-band filter that is odd-length where all odd index coefficients are
     * zero valued except for the middle odd index coefficient which should be valued 0.5
     * @param gain value to apply to the output
     */
    public HalfBandFilter2(float[] coefficients, float gain)
    {
        if(coefficients.length % 2 == 0)
        {
            throw new IllegalArgumentException("Half-band filter coefficients must be odd-length");
        }

        mGain = gain;

        int half = coefficients.length / 2 + 1;

        mCoefficients = new float[half];
        mEvenSamples = new float[half];

        //Use only the even coefficients since the odd coefficients are all zero-valued
        for(int x = 0; x < coefficients.length; x += 2)
        {
            mCoefficients[x / 2] = coefficients[x];
        }

        //Setup the odd samples array as a simple delay line, half the size of the even samples array
        mOddSamples = new float[half / 2];
        mCenterSampleIndex = mOddSamples.length - 1;
    }

    public float filter(float sample1, float sample2)
    {
        //Structured for SIMD array copy optimization when supported by host CPU
        System.arraycopy(mEvenSamples, 0, mEvenSamples, 1, mEvenSamples.length - 1);
        System.arraycopy(mOddSamples, 0, mOddSamples, 1, mOddSamples.length - 1);

        mEvenSamples[0] = sample1;
        mOddSamples[0] = sample2;

        mAccumulator = 0.0f;

        //Structured for SIMD dot.product optimization when supported by host CPU
        for(mPointer = 0; mPointer < mEvenSamples.length; mPointer++)
        {
            mAccumulator += (mEvenSamples[mPointer] * mCoefficients[mPointer]);
        }

        mAccumulator += (mOddSamples[mCenterSampleIndex] * CENTER_COEFFICIENT);
        mAccumulator *= mGain;

        return mAccumulator;
    }
}
