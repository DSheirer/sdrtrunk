/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package buffer;

/**
 * Circular sample buffer - allocates a buffer and stores samples in a circular
 * fashion, overwriting older samples with newly arrived samples.
 */
public class ReverseFloatCircularBuffer
{
    private float[] mBuffer;
    private int mBufferPointer = 0;

    public ReverseFloatCircularBuffer(int size)
    {
        mBuffer = new float[size];
        mBufferPointer = mBuffer.length - 1;
    }

    /**
     * Size of the buffer
     * @return
     */
    public int getSize()
    {
        return mBuffer.length;
    }

    public float[] getBuffer()
    {
        return mBuffer;
    }

    /**
     * Adds the new value to this buffer.
     */
    public void put(float newValue)
    {
        mBuffer[mBufferPointer--] = newValue;

        if(mBufferPointer < 0)
        {
            mBufferPointer = mBuffer.length - 1;
        }
    }

    /**
     * Returns the sample at the specified index, where index 0 is the newest sample and index (length - 1) is the
     * oldest sample.
     * @param index in range of 0 to length-1
     * @return value at indexed position
     */

    public float get(int index)
    {
        if(index < mBuffer.length)
        {
            int pointer = mBufferPointer + 1 + index;

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
}
