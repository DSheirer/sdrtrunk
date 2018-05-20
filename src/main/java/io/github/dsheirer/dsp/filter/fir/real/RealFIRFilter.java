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

@Deprecated //Use RealFIRFilter2 instead
public class RealFIRFilter extends FIRFilter
{
    private float[] mBuffer;
    private int mBufferSize = 1;
    private int mBufferPointer = 0;
    private int[][] mIndexMap;

    private float[] mCoefficients;
    private float mGain;
    private float mAccumulator;

    /**
     * Float sample FIR filter base class.
     *
     * @param coefficients - filter coefficients
     * @param gain value to apply to the filtered output - use 1.0f for no gain
     */
    public RealFIRFilter(float[] coefficients, float gain)
    {
        mCoefficients = coefficients;
        mGain = gain;

        mBufferSize = mCoefficients.length;

        mBuffer = new float[mBufferSize];
        mBufferPointer = mBufferSize - 1;

        generateIndexMap(mBufferSize);
    }

    public float[] getCoefficients()
    {
        return mCoefficients;
    }

    @Override
    public void dispose()
    {
        mCoefficients = null;
        mIndexMap = null;
        mBuffer = null;
    }

    /**
     * Loads the sample into the circular buffer without calculating a filtered value.
     * @param sample to load
     */
    public void load(float sample)
    {
        mBuffer[mBufferPointer] = sample;

        mBufferPointer--;

        if(mBufferPointer < 0)
        {
            mBufferPointer += mBufferSize;
        }
    }

    /**
     * Loads the sample into the circular buffer and calculates a filtered sample output.
     * @param sample to load
     * @return filtered value
     */
    public float filter(float sample)
    {
        mBuffer[mBufferPointer] = sample;

        mAccumulator = 0.0f;

        for(int x = 0; x < mBufferSize; x++)
        {
            mAccumulator += mCoefficients[x] * mBuffer[mIndexMap[mBufferPointer][x]];
        }

        mBufferPointer--;

        if(mBufferPointer < 0)
        {
            mBufferPointer += mBufferSize;
        }

        /* Apply gain and return the filtered value */
        mAccumulator *= mGain;

        return mAccumulator;
    }

    /**
     * Current output value for the filter after the filter() method has been invoked.
     * @return
     */
    public float currentValue()
    {
        return mAccumulator;
    }

    /**
     * Generates a circular buffer index map to support lookup of the translated
     * index based on the current buffer pointer and the desired sample index.
     */
    private void generateIndexMap(int size)
    {
        mIndexMap = new int[size][size];

        for(int x = 0; x < size; x++)
        {
            for(int y = 0; y < size; y++)
            {
                int z = x + y;

                mIndexMap[x][y] = z < size ? z : z - size;
            }
        }
    }
}