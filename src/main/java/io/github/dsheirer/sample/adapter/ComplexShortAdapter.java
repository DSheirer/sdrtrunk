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
package io.github.dsheirer.sample.adapter;

import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Converts 16-bit little endian byte data into an array of complex floats
 */
public class ComplexShortAdapter implements IComplexSampleAdapter
{
    private ShortToFloatMap mMap = new ShortToFloatMap();
    private FloatBuffer mFloatBuffer;
    private ByteOrder mByteOrder = ByteOrder.LITTLE_ENDIAN;

    @Override
    public void convertAndLoad(byte[] samples, ReusableComplexBuffer reusableComplexBuffer)
    {
        int sampleCount = samples.length / 2;

        if(mFloatBuffer == null || mFloatBuffer.capacity() != sampleCount)
        {
            mFloatBuffer = FloatBuffer.allocate(sampleCount);
        }

        mFloatBuffer.rewind();

        /* Wrap byte array in a byte buffer so we can process them as shorts */
        ByteBuffer buffer = ByteBuffer.wrap(samples);

        /* Set endian to correct byte ordering */
        buffer.order(mByteOrder);

        while(buffer.hasRemaining())
        {
            mFloatBuffer.put(mMap.get(buffer.getShort()));
        }

        reusableComplexBuffer.reloadFrom(mFloatBuffer, System.currentTimeMillis());
    }
}
