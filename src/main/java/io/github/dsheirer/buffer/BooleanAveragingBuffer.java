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

/**
 * Circular Buffer - implements a circular buffer with a method to get the
 * current head of the buffer, and put a new value in its place at the same
 * time
 *
 * Also provides averaging over the elements in the buffer
 */
public class BooleanAveragingBuffer
{
    private boolean[] mBuffer;
    private int mBufferPointer;
    private int mThreshold;
    private int mTrueCount;

    public BooleanAveragingBuffer(int length)
    {
        mBuffer = new boolean[length];
        mThreshold = length / 2;
    }

    public boolean get(boolean newValue)
    {
        //Fetch current boolean value
        boolean retVal = mBuffer[mBufferPointer];

        //Load the new value into the buffer
        put(newValue);

        return retVal;
    }

    /**
     * Loads the newValue into this buffer and adjusts the buffer pointer
     * to prepare for the next get/put cycle
     */
    private void put(boolean newValue)
    {
        //Store the new value to the buffer
        mBuffer[mBufferPointer] = newValue;

        //Increment the buffer pointer
        mBufferPointer++;

        mBufferPointer = mBufferPointer % mBuffer.length;
    }

    /**
     * Loads the newValue into the buffer, calculates the average
     * and returns that average from this method
     *
     * This effectively performs low-pass filtering
     */
    public boolean getAverage(boolean newValue)
    {
        //Load the new value into the buffer
        put(newValue);

        mTrueCount = 0;

        for (boolean b : mBuffer) {
            if (b) {
                mTrueCount++;
            }
        }

        return mTrueCount > mThreshold;
    }
}
