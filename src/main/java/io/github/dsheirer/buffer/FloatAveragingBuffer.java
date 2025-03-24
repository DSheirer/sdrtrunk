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

import java.util.Arrays;

/**
 * Averaging buffer backed by a ring buffer implemented with a float array.
 */
public class FloatAveragingBuffer
{
    private final float[] mBuffer;
    private final int mBufferSize;
    private float mAverage = 0.0f;
    private int mBufferPointer;
    private boolean mRapidFill = false;
    private int mRapidFillCounter = 0;
    private int mRapidFillIncrement;

    /**
     * Constructs an instance that uses a rapid fill increment value where the initial values get counted multiple times
     * to more quickly get to the average value instead of walking up to the average from the default average value of 0.
     * @param size of buffer
     * @param rapidFillIncrement indicating how many value additions per individual add to execute until the averaging
     * buffer is filled.  After the initial fill state is achieved, reverts to using a single value addition.
     */
    public FloatAveragingBuffer(int size, int rapidFillIncrement)
    {
        this(size);
        mRapidFillIncrement = rapidFillIncrement;
        mRapidFill = true;
    }

    /**
     * Constructs an instance that uses a ring buffer of the specified size.
     * @param size of the averaging buffer.
     */
    public FloatAveragingBuffer(int size)
    {
        mBufferSize = size;
        mBuffer = new float[size];
    }

    /**
     * Resets this buffer to an empty state
     */
    public void reset()
    {
        Arrays.fill(mBuffer, 0);
        mBufferPointer = 0;
        mAverage = 0.0f;

        if(mRapidFillIncrement > 0)
        {
            mRapidFill = true;
            mRapidFillCounter = 0;
        }
    }

    /**
     * Current average value.
     * @return average
     */
    public float getAverage()
    {
        return mAverage;
    }

    /**
     * Adds the value to the buffer and updates the average.
     * @param value to add
     */
    public void add(float value)
    {
        if(mRapidFill)
        {
            for(int x = 0; x < mRapidFillIncrement; x++)
            {
                float oldValue = mBuffer[mBufferPointer];

                if(Float.isInfinite(value) || Float.isNaN(value))
                {
                    mAverage = mAverage - (oldValue / mBufferSize);
                    mBuffer[mBufferPointer++] = 0.0f;
                }
                else
                {
                    mAverage = mAverage - (oldValue / mBufferSize) + (value / mBufferSize);
                    mBuffer[mBufferPointer++] = value;
                }

                if(mBufferPointer >= mBufferSize)
                {
                    mBufferPointer = 0;
                }
            }

            mRapidFillCounter += mRapidFillIncrement;

            if(mRapidFillCounter > mBufferSize)
            {
                mRapidFill = false;
            }
        }
        else
        {
            float oldValue = mBuffer[mBufferPointer];

            if(Float.isInfinite(value) || Float.isNaN(value))
            {
                mAverage = mAverage - (oldValue / mBufferSize);
                mBuffer[mBufferPointer++] = 0.0f;
            }
            else
            {
                mAverage = mAverage - (oldValue / mBufferSize) + (value / mBufferSize);
                mBuffer[mBufferPointer++] = value;
            }

            if(mBufferPointer >= mBufferSize)
            {
                mBufferPointer = 0;
            }
        }
    }

    /**
     * Adds the value to the buffer and calculates a new average.
     * @param value to add
     * @return updated average
     */
    public float get(float value)
    {
        add(value);
        return mAverage;
    }
}
