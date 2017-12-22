/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
 ******************************************************************************/
package io.github.dsheirer.dsp.filter.polyphase;

public class FilterStage
{
    private float[] mBuffer;
    private int mBufferSize;
    private int mBufferPointer;
    private int[][] mIndexMap;
    private float[] mCoefficients;

    /**
     * Single stage of a polyphase filter implemented with a circular sample buffer that uses an index map to ensure
     * correct alignment of samples to filter taps.
     *
     * @param coefficients
     */
    public FilterStage(float[] coefficients)
    {
        mCoefficients = coefficients;

        mBufferSize = mCoefficients.length;

        mBuffer = new float[mBufferSize];

        //Note: buffer pointer starts at an illegal index but is decremented before first usage
        mBufferPointer = mBufferSize;

        generateIndexMap(mBufferSize);
    }

    /**
     * Coefficients used by this filter stage.
     */
    public float[] getCoefficients()
    {
        return mCoefficients;
    }

    /**
     * Cleanup prior to disposal
     */
    public void dispose()
    {
        mCoefficients = null;
        mIndexMap = null;
        mBuffer = null;
    }

    /**
     * Loads the sample into the sample buffer
     */
    private void load(float sample)
    {
        mBufferPointer--;

        if (mBufferPointer < 0)
        {
            mBufferPointer += mBufferSize;
        }

        mBuffer[mBufferPointer] = sample;
    }

    /**
     * Applies convolution against the currently loaded buffer samples.
     */
    public float filter(float sample)
    {
        load(sample);

        float accumulator = 0.0f;

        for (int x = 0; x < mBufferSize; x++)
        {
            accumulator += mCoefficients[x] * mBuffer[mIndexMap[mBufferPointer][x]];
        }

        return accumulator;
    }

    /**
     * Generates a circular buffer index map to support lookup of the translated index based on the current buffer
     * pointer and the desired sample index.
     */
    private void generateIndexMap(int size)
    {
        mIndexMap = new int[size][size];

        for (int x = 0; x < size; x++)
        {
            for (int y = 0; y < size; y++)
            {
                int z = x + y;

                mIndexMap[x][y] = z < size ? z : z - size;
            }
        }
    }
}