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
package io.github.dsheirer.source.tuner.usb.converter;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class SignedByteSampleConverter extends NativeBufferConverter
{
    private final static float[] LOOKUP_VALUES;

    //Creates a static lookup table that converts the signed 8-bit valued range from -128 to 127 into scaled float values
    //of -1.0 to 0 to 1.0
    static
    {
        LOOKUP_VALUES = new float[256];

        for(int x = 0; x < 256; x++)
        {
            LOOKUP_VALUES[x] = (float)((byte)x) / 128.0f;
        }
    }

    private FloatBuffer mFloatBuffer;

    /**
     * Converts native byte buffers containing signed 8-bit complex samples into complex float samples loaded into a tracked,
     * reusable complex sample buffer.  Internally tracks the reusable buffer until all downstream consumers have finished
     * processing the buffer contents and then reuses the buffer (and memory) for subsequent samples.
     */
    public SignedByteSampleConverter()
    {
    }

    /**
     * Converts the signed 8-bit complex samples contained in the native buffer into floats that are loaded into a float
     * buffer and subsequently transferred to a reusable complex buffer by the parent class.
     *
     * @param nativeBuffer containing signed 8-bit complex samples
     * @param length of bytes to read from the native buffer
     * @return float buffer loaded with converted samples
     */
    @Override
    protected FloatBuffer convertSamples(ByteBuffer nativeBuffer, int length)
    {
        nativeBuffer.rewind();

        if(mFloatBuffer == null || mFloatBuffer.capacity() != nativeBuffer.capacity())
        {
            mFloatBuffer = FloatBuffer.allocate(nativeBuffer.capacity());
        }

        mFloatBuffer.rewind();

        int count = 0;

        while(nativeBuffer.hasRemaining() && count < length)
        {
            byte sample = nativeBuffer.get();
            count++;

            mFloatBuffer.put(LOOKUP_VALUES[(sample & 0xFF)]);
        }

        return mFloatBuffer;
    }

    public static void main(String[] args)
    {
        SignedByteSampleConverter converter = new SignedByteSampleConverter();
        int a = 0;
    }
}
