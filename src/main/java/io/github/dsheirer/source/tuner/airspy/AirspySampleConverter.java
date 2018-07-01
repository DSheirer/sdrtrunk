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
package io.github.dsheirer.source.tuner.airspy;

import io.github.dsheirer.dsp.filter.dc.DCRemovalFilter;
import io.github.dsheirer.dsp.filter.hilbert.HilbertTransform;
import io.github.dsheirer.source.tuner.usb.converter.NativeBufferConverter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class AirspySampleConverter extends NativeBufferConverter
{
    private static final float SCALE_SIGNED_12_BIT_TO_FLOAT = 1.0f / 2048.0f;

    private DCRemovalFilter mDCFilter = new DCRemovalFilter(0.01f);
    private HilbertTransform mHilbertTransform = new HilbertTransform();
    private boolean mSamplePacking = false;
    private FloatBuffer mFloatBuffer;
    private float[] mConvertedSamples;

    /**
     * Adapter to translate byte buffers received from the airspy tuner into
     * float buffers for processing.
     */
    public AirspySampleConverter()
    {
    }

    @Override
    protected FloatBuffer convertSamples(ByteBuffer buffer, int length)
    {
        float[] samples = convert(buffer);

        if(mFloatBuffer == null || mFloatBuffer.capacity() != samples.length)
        {
            mFloatBuffer = FloatBuffer.allocate(samples.length);
        }

        mFloatBuffer.rewind();
        mFloatBuffer.put(samples);

        return mFloatBuffer;
    }

    /**
     * Sample packing places two 12-bit samples into 3 bytes when enabled or
     * places two 12-bit samples into 4 bytes when disabled.
     *
     * @param enabled
     */
    public void setSamplePacking(boolean enabled)
    {
        mSamplePacking = enabled;
    }

    private float[] convert(ByteBuffer samples)
    {
        if(mSamplePacking)
        {
            convertPacked(samples.order(ByteOrder.LITTLE_ENDIAN));
        }
        else
        {
            convertUnpacked(samples);
        }

        mDCFilter.filter(mConvertedSamples);

        return mHilbertTransform.filter(mConvertedSamples);
    }

    /**
     * Converts the byte array containing unsigned 12-bit short values into
     * signed float values in the range -1 to 1;
     *
     * @param buffer - native byte buffer containing unsigned 16-bit values
     */
    private void convertUnpacked(ByteBuffer buffer)
    {
        buffer.rewind();

        if(mConvertedSamples == null || mConvertedSamples.length != buffer.capacity() / 2)
        {
            mConvertedSamples = new float[buffer.capacity() / 2];
        }

        int pointer = 0;


        while(buffer.remaining() >= 2)
        {
            byte lsb = buffer.get();
            byte msb = buffer.get();
            mConvertedSamples[pointer++] = scale((lsb & 0xFF) | (msb << 8));
        }
    }

    /**
     * Converts every 3 bytes containing a pair of 12-bit unsigned values into
     * a pair of float values in the range -1 to 1;
     *
     * @param buffer - native byte buffer containing packet 12-bit unsigned samples
     */
    private void convertPacked(ByteBuffer buffer)
    {
        buffer.rewind();

        int sampleCount = buffer.capacity() / 3 * 2;

        if(mConvertedSamples == null || mConvertedSamples.length != sampleCount)
        {
            mConvertedSamples = new float[sampleCount];
        }

        int pointer = 0;

        byte b1;
        byte b2;
        byte b3;
        int first;
        int second;

        while(buffer.remaining() >= 3)
        {
            b1 = buffer.get();
            b2 = buffer.get();
            b3 = buffer.get();

            first = ((b1 << 4) & 0xFF0) | ((b2 >> 4) & 0xF);
            mConvertedSamples[pointer++] = scale(first);

            second = ((b2 << 8) & 0xF00) | (b3 & 0xFF);
            mConvertedSamples[pointer++] = scale(second);
        }
    }

    /**
     * Converts unsigned 12-bit values to signed 12-bit values and then scales
     * the signed value to a signed float value in range: -1.0 : +1.0
     */
    public static float scale(int value)
    {
        return (float) ((value & 0xFFF) - 2048) * SCALE_SIGNED_12_BIT_TO_FLOAT;
    }
}
