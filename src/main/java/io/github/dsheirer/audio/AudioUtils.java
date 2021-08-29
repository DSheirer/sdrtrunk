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
package io.github.dsheirer.audio;

import io.github.dsheirer.sample.ConversionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.Float;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class AudioUtils
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioUtils.class);

    /**
     * Converts the audio packets into a byte array of 16-bit, little-endian audio samples
     */
    public static byte[] convertTo16BitSamples(List<float[]> buffers)
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try
        {
            for(float[] audioBuffer: buffers)
            {
                //Converting from 32-bit floats to signed 16-bit samples
                ByteBuffer buffer = ConversionUtils.convertToSigned16BitSamples(audioBuffer);
                stream.write(buffer.array());
            }
        }
        catch(IOException e)
        {
            mLog.error("Error writing converted PCM bytes to output stream");
        }

        return stream.toByteArray();
    }

    /**
     * Converts the audio packets into a byte array of 16-bit, little-endian audio samples
     */
    public static byte[] convert(List<float[]> audioBuffers)
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try
        {
            for(float[] audioBuffer: audioBuffers)
            {
                //Converting from 32-bit floats to signed 16-bit samples
                ByteBuffer buffer = ConversionUtils.convertToSigned16BitSamples(audioBuffer);
                stream.write(buffer.array());
            }
        }
        catch(IOException e)
        {
            mLog.error("Error writing converted PCM bytes to output stream");
        }

        return stream.toByteArray();
    }

    /**
     * Normalize audio samples
     */
    public static List<float[]> normalize(List<float[]> audioBuffers)
    {
        float targetMax = 0.95f; // -0.44dBFS
        float max = 0.1f; // Initialize at 0.1 (-20dBFS) to limit maximum gain

        for(float[] audioBuffer: audioBuffers)
        {
            for(float value: audioBuffer)
            {
                float newMax = Math.max(max, Math.abs(value));
                if(!Float.isNaN(newMax))
                {
                    max = newMax;
                }
            }
        }

        if(targetMax == max)
        {
            return audioBuffers;
        }

        float gain = targetMax / max;

        for(float[] audioBuffer: audioBuffers)
        {
            for(int i=0;i<audioBuffer.length;i++)
            {
                audioBuffer[i] *= gain;
            }
        }

        return audioBuffers;
    }

}
