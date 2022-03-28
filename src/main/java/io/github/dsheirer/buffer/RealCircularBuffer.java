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
package io.github.dsheirer.buffer;

/**
 * Circular sample buffer - allocates a buffer and stores samples in a circular
 * fashion, overwriting older samples with newly arrived samples.
 *
 * Can be used as a delay-type buffer, to delay samples by the 'size' amount
 */
public class RealCircularBuffer
{
    float[] mBuffer;
    int mBufferPointer = 0;

    public RealCircularBuffer(int size)
    {
        mBuffer = new float[size];
    }

    public int getSize()
    {
        return mBuffer.length;
    }

    public float[] getBuffer()
    {
        return mBuffer;
    }

    /**
     * Puts the new value into the buffer and returns the oldest buffer value
     * that it replaced
     *
     * @param newValue
     * @return
     */
    public float putAndGet(float newValue)
    {
        float oldestSample = mBuffer[mBufferPointer];

        put(newValue);

        return oldestSample;
    }

    /**
     * Adds the new value to this buffer.
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

    /**
     * Returns the sample at the specified index, where index 0 is the oldest sample and index (length - 1) is the
     * newest sample.
     * @param index in range of 0 to length-1
     * @return value at indexed position
     */

    public float get(int index)
    {
        if(index < mBuffer.length)
        {
            int pointer = mBufferPointer + index;

            if(pointer >= mBuffer.length)
            {
                pointer -= mBuffer.length;
            }

            return mBuffer[pointer];
        }
        else
        {
            throw new ArrayIndexOutOfBoundsException("Index [" + index + "] is not valid for the buffer size of [" +
                mBuffer.length + "]");
        }
    }

    /**
     * Returns the contents of the circular buffer unwrapped where index 0 is the oldest sample and index length-1
     * is the newest sample.
     *
     * @return array of samples
     */
    public float[] get()
    {
        float[] samples = new float[mBuffer.length];

        System.arraycopy(mBuffer, mBufferPointer, samples, 0, mBuffer.length - mBufferPointer);

        if(mBufferPointer != 0)
        {
            System.arraycopy(mBuffer, 0, samples, mBuffer.length - mBufferPointer, mBufferPointer);
        }

        return samples;
    }
}
