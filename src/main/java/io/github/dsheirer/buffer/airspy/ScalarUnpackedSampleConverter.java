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

package io.github.dsheirer.buffer.airspy;

import java.nio.ByteBuffer;

/**
 * Scalar implementation of airspy sample converter for un-packed samples.
 */
public class ScalarUnpackedSampleConverter implements IAirspySampleConverter
{
    private static final float DC_FILTER_GAIN = 0.007f; //Normalizes DC over period of ~142 (1 / .007) buffers
    private float mAverageDc = 0.0f;

    @Override
    public short[] convert(ByteBuffer buffer)
    {
        int offset = 0;
        long dcAccumulator = 0;
        short sample;
        short[] samples;
        byte b1, b2;

        samples = new short[buffer.capacity() / 2];

        for(int x = 0; x < samples.length; x++)
        {
            b1 = buffer.get(offset++);
            b2 = buffer.get(offset++);
            sample = (short)(((b2 & 0x0F) << 8) | (b1 & 0xFF));
            samples[x] = sample;
            dcAccumulator += sample;
        }

        //Calculate the average scaled DC offset so that it can be applied in the native buffer's converted samples
        float averageDcNow = ((float)dcAccumulator / (float)samples.length) - 2048.0f;
        averageDcNow *= AirspyBufferIterator.SCALE_SIGNED_12_BIT_TO_FLOAT;
        averageDcNow -= mAverageDc;
        mAverageDc += (averageDcNow * DC_FILTER_GAIN);

        return samples;
    }

    @Override
    public float getAverageDc()
    {
        return mAverageDc;
    }
}
