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
package io.github.dsheirer.buffer;

import java.util.Arrays;

/**
 * Circular sample buffer - allocates a buffer and stores samples in a circular
 * fashion, overwriting older samples with newly arrived samples.  Initially
 * fills buffer with 0-valued samples
 *
 * Can be used as a delay-type buffer, to delay samples by the 'size' amount
 */
public class FloatCircularBuffer
{
    private float[] mBuffer;
    private int mBufferPointer = 0;
    private float mOldestSample;

    /**
     * Creates a circular double buffer of the specified size and all entries filled with the specified initial value.
     *
     * @param size of the buffer
     * @param initialFillValue to fill the buffer entries
     */
    public FloatCircularBuffer(int size, float initialFillValue)
    {
        mBuffer = new float[size];
        Arrays.fill(mBuffer, initialFillValue);
    }

    /**
     * Creates a circular double buffer of the specified size and all entries filled with an initial value of zero.
     *
     * @param size of the buffer
     */
    public FloatCircularBuffer(int size)
    {
        this(size, 0.0f);
    }

    public float[] getBuffer()
    {
        return mBuffer;
    }

    /**
     * Gets the current buffer value and overwrites that value position in the buffer with the new value.
     *
     * @param newValue to add to the buffer
     * @return oldest value that was overwritten by the new value
     */
    public float getAndPut(float newValue)
    {
        mOldestSample = mBuffer[mBufferPointer];
        put(newValue);
        return mOldestSample;
    }

    /**
     * Puts the value into the buffer, updates the pointer and returns the buffer value at the pointer position.
     * @param sample to add
     * @return next pointed sample.
     */
    public float putAndGet(float sample)
    {
        put(sample);
        return mBuffer[mBufferPointer];
    }

    public float[] getAll()
    {
        int valuePointer = 0;
        float[] values = new float[mBuffer.length];
        int bufferPointer = mBufferPointer;

        while(valuePointer < values.length)
        {
            values[valuePointer++] = mBuffer[bufferPointer++];

            if(bufferPointer >= mBuffer.length)
            {
                bufferPointer = 0;
            }
        }

        return values;
    }

    /**
     * Places the new value into the buffer, overwriting the oldest value
     *
     * @param newValue to add to the buffer
     */
    public void put(float newValue)
    {
        mBuffer[mBufferPointer] = newValue;
        mBufferPointer++;
        mBufferPointer %= mBuffer.length;
    }
}
