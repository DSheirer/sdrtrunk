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
package io.github.dsheirer.sample.buffer;

import org.apache.commons.lang3.Validate;

public class ByteBuffer extends AbstractBuffer
{
    private byte[] mSamples;

    /**
     * Creates a reusable, timestamped byte buffer using the specified time in milliseconds.
     *
     * NOTE: reusability of this buffer requires strict user count tracking.  Each component that receives this
     * buffer should not modify the buffer contents.  Each component should also increment the user count before
     * sending this buffer to another component and should decrement the user count when finished using this buffer.
     *  @param bytes of data
     * @param timestamp in millis for the buffer
     */
    ByteBuffer(byte[] bytes, long timestamp)
    {
        super(timestamp);
        mSamples = bytes;
    }

    /**
     * Constructs a timestamped byte buffer using the current system time in milliseconds.
     *
     * NOTE: reusability of this buffer requires strict user count tracking.  Each component that receives this
     * buffer should not modify the buffer contents.  Each component should also increment the user count before
     * sending this buffer to another component and should decrement the user count when finished using this buffer.
     *
     * @param bytes of data
     */
    public ByteBuffer(byte[] bytes)
    {
        this(bytes, System.currentTimeMillis());
    }

    /**
     * Samples for this buffer
     */
    public byte[] getBytes()
    {
        return mSamples;
    }

    /**
     * Creates a copy of the samples from this buffer
     */
    public byte[] getSamplesCopy()
    {
        byte[] bytes = getBytes();
        byte[] copy = new byte[bytes.length];
        System.arraycopy(bytes, 0, copy, 0, bytes.length);
        return copy;
    }

    /**
     * Number of bytes contained in this buffer
     */
    public int getSampleCount()
    {
        return getBytes().length;
    }

    /**
     * Resizes the internal array to the size argument
     *
     * @param size for the internal array
     */
    protected void resize(int size)
    {
        if(mSamples.length != size)
        {
            mSamples = new byte[size];
        }
    }
}
