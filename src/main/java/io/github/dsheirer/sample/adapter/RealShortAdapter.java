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

import io.github.dsheirer.sample.buffer.ReusableBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Converts 16-bit/2-byte little endian byte data into a reusable buffer of real float sample data
 */
public class RealShortAdapter extends RealSampleAdapter
{
    private ByteBuffer mByteBuffer;
    private ShortToFloatMap mMap = new ShortToFloatMap();
    private ByteOrder mByteOrder = ByteOrder.LITTLE_ENDIAN;

    /**
     * Constructs a real sample adapter
     *
     * @param debugName to use for debug logging
     */
    public RealShortAdapter(String debugName)
    {
        super(debugName);
    }

    @Override
    public ReusableBuffer convert(byte[] samples)
    {
        ReusableBuffer reusableBuffer = getBuffer(samples.length / 2);
        float[] convertedSamples = reusableBuffer.getSamples();

        int pointer = 0;

        mByteBuffer = ByteBuffer.wrap(samples);

        /* Set endian to correct byte ordering */
        mByteBuffer.order(mByteOrder);

        while(mByteBuffer.hasRemaining())
        {
            convertedSamples[pointer] = mMap.get(mByteBuffer.getShort());
            pointer++;
        }

        return reusableBuffer;
    }

    /**
     * Set byte interpretation to little or big endian.  Defaults to LITTLE
     * endian
     */
    public void setByteOrder(ByteOrder order)
    {
        mByteOrder = order;
    }
}
