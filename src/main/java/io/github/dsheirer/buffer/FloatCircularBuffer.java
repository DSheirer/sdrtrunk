/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.buffer;

import java.util.Arrays;

/**
 * Circular sample buffer - allocates a buffer and stores samples in a circular
 * fashion, overwriting older samples with newly arrived samples.  Initially
 * fills buffer with 0-valued samples
 *
 * Can be used as a delay-type buffer, to delay samples by the 'size' amount
 *
 * @author denny
 */
public class FloatCircularBuffer
{
    float[] mBuffer;
    int mBufferPointer = 0;

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
     * Puts the new value into the buffer and returns the oldest buffer value that it replaced
     *
     * @param newValue to add to the buffer
     * @return oldest value that was overwritten by the new value
     */
    public float get(float newValue)
    {
        float oldestSample = mBuffer[mBufferPointer];

        mBuffer[mBufferPointer] = newValue;

        mBufferPointer++;

        if(mBufferPointer >= mBuffer.length)
        {
            mBufferPointer = 0;
        }

        return oldestSample;
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

        if(mBufferPointer >= mBuffer.length)
        {
            mBufferPointer = 0;
        }
    }
}
