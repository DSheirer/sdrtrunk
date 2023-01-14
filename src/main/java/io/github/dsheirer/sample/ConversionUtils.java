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

package io.github.dsheirer.sample;

import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Utilities for converting to/from signed 16-bit sample byte arrays and float buffers
 */
public class ConversionUtils
{
    public static float[] convertFromSigned16BitSamples(byte[] bytes)
    {
        return convertFromSigned16BitSamples(ByteBuffer.wrap(bytes));
    }

    /**
     * Converts the byte buffer containing 16-bit samples into a float array
     */
    public static float[] convertFromSigned16BitSamples(ByteBuffer buffer)
    {
        ShortBuffer byteBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();

        float[] samples = new float[buffer.limit() / 2];

        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = (float)byteBuffer.get() / (float)Short.MAX_VALUE;
        }

        return samples;
    }

    /**
     * Converts the float samples into a little-endian 16-bit sample byte buffer.
     *
     * @param samples - float array of sample data
     * @return - little-endian 16-bit sample byte buffer
     */
    public static ByteBuffer convertToSigned16BitSamples(float[] samples)
    {
        ByteBuffer converted = ByteBuffer.allocate(samples.length * 2);
        converted.order(ByteOrder.LITTLE_ENDIAN);

        for(float sample : samples)
        {
            if(sample > 1.0f)
            {
                converted.putShort(Short.MAX_VALUE);
            }
            else if(sample < -1.0f)
            {
                converted.putShort((short)-Short.MAX_VALUE);
            }
            else
            {
                converted.putShort((short)(sample * Short.MAX_VALUE));
            }
        }

        return converted;
    }

    /**
     * Converts the float samples into a little-endian 32-bit sample byte buffer.
     *
     * @param samples - float array of sample data
     * @return - little-endian 32-bit sample byte buffer
     */
    public static ByteBuffer convertToSigned32BitSamples(float[] samples)
    {
        ByteBuffer converted = ByteBuffer.allocate(samples.length * 4);
        converted.order(ByteOrder.LITTLE_ENDIAN);

        for(float sample : samples)
        {
            if(sample > 1.0f)
            {
                converted.putInt(Integer.MAX_VALUE);
            }
            else if(sample < -1.0f)
            {
                converted.putInt(-Integer.MAX_VALUE);
            }
            else
            {
                converted.putInt((int)(sample * Integer.MAX_VALUE));
            }
        }

        return converted;
    }

    /**
     * Converts the float samples into a little-endian 16-bit sample byte buffer.
     *
     * @param samples - float array of sample data
     * @return - little-endian 16-bit sample byte buffer
     */
    public static ByteBuffer convertToSigned16BitSamples(ComplexSamples samples)
    {
        ByteBuffer converted = ByteBuffer.allocate(samples.i().length * 4);
        converted.order(ByteOrder.LITTLE_ENDIAN);

        for(int x = 0; x < samples.i().length; x++)
        {
            converted.putShort((short)(samples.i()[x] * Short.MAX_VALUE));
            converted.putShort((short)(samples.q()[x] * Short.MAX_VALUE));
        }

        return converted;
    }

    /**
     * Converts the float samples in a complex buffer to a little endian 16-bit
     * buffer
     */
    public static ByteBuffer convertToSigned16BitSamples(InterleavedComplexSamples complexSamples)
    {
        return convertToSigned16BitSamples(complexSamples.samples());
    }
}
