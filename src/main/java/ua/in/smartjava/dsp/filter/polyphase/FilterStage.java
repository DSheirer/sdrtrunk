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
package ua.in.smartjava.dsp.filter.polyphase;

public class FilterStage
{
    private float[] mBuffer;
    private int mBufferSize;
    private int mBufferPointer;
    private int[][] mIndexMap;
    private float[] mCoefficients;

    /**
     * Single stage of a polyphase ua.in.smartjava.filter implemented with a circular ua.in.smartjava.sample ua.in.smartjava.buffer that uses an index ua.in.smartjava.map to ensure
     * correct alignment of samples to ua.in.smartjava.filter taps.
     *
     * @param coefficients
     */
    public FilterStage(float[] coefficients)
    {
        mCoefficients = coefficients;

        mBufferSize = mCoefficients.length;

        mBuffer = new float[mBufferSize];

        //Note: ua.in.smartjava.buffer pointer starts at an illegal index but is decremented before first usage
        mBufferPointer = mBufferSize;

        generateIndexMap(mBufferSize);
    }

    /**
     * Coefficients used by this ua.in.smartjava.filter stage.
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
     * Loads the ua.in.smartjava.sample into the ua.in.smartjava.sample ua.in.smartjava.buffer
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
     * Applies convolution against the currently loaded ua.in.smartjava.buffer samples.
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
     * Generates a circular ua.in.smartjava.buffer index ua.in.smartjava.map to support lookup of the translated index based on the current ua.in.smartjava.buffer
     * pointer and the desired ua.in.smartjava.sample index.
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