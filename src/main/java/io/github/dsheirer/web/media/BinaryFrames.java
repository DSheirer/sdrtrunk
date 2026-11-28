/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.web.media;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Binary WebSocket frames for the spectrum and audio.live topics.  Little-endian.  Every frame starts with:
 * <pre>
 * offset 0  uint8   version (1)
 * offset 1  uint8   kind: 1 spectrum, 2 audio
 * offset 2  uint16  reserved (0)
 * offset 4  uint32  subscription id
 * </pre>
 * Spectrum (kind 1):
 * <pre>
 * offset 8  int64   timestamp, epoch milliseconds
 * offset 16 int64   center frequency, Hz
 * offset 24 uint32  sample rate (displayed bandwidth), Hz
 * offset 28 uint32  bin count
 * offset 32 float32 dB of bin value 0
 * offset 36 float32 dB of bin value 255
 * offset 40 uint8[] bins, lowest frequency first; dB = min + value / 255 * (max - min)
 * </pre>
 * Audio (kind 2):
 * <pre>
 * offset 8  int64   call key (the callKey of the calls topic)
 * offset 16 uint32  frame sequence within the call, from 0
 * offset 20 byte[]  one Opus packet: 20 ms, 8 kHz mono
 * </pre>
 */
public final class BinaryFrames
{
    public static final int VERSION = 1;
    public static final int KIND_SPECTRUM = 1;
    public static final int KIND_AUDIO = 2;
    public static final int SPECTRUM_HEADER = 40;
    public static final int AUDIO_HEADER = 20;

    private BinaryFrames()
    {
    }

    /**
     * Quantized spectrum bins, shared by every subscriber of a source
     * @param bins uint8 values
     * @param minDb dB of value 0
     * @param maxDb dB of value 255
     */
    public record QuantizedBins(byte[] bins, float minDb, float maxDb)
    {
        /**
         * Quantizes dB values to 0-255 between the frame's lowest and highest value.
         */
        public static QuantizedBins of(float[] decibels)
        {
            float min = Float.MAX_VALUE;
            float max = -Float.MAX_VALUE;

            for(float value: decibels)
            {
                if(value < min)
                {
                    min = value;
                }

                if(value > max)
                {
                    max = value;
                }
            }

            if(max - min < 1.0f)
            {
                max = min + 1.0f;
            }

            float scale = 255.0f / (max - min);
            byte[] bins = new byte[decibels.length];

            for(int x = 0; x < decibels.length; x++)
            {
                bins[x] = (byte)Math.round((decibels[x] - min) * scale);
            }

            return new QuantizedBins(bins, min, max);
        }
    }

    public static byte[] spectrum(int sub, long timestamp, long centerFrequency, long sampleRate, QuantizedBins bins)
    {
        ByteBuffer buffer = ByteBuffer.allocate(SPECTRUM_HEADER + bins.bins().length).order(ByteOrder.LITTLE_ENDIAN);
        header(buffer, KIND_SPECTRUM, sub);
        buffer.putLong(timestamp);
        buffer.putLong(centerFrequency);
        buffer.putInt((int)sampleRate);
        buffer.putInt(bins.bins().length);
        buffer.putFloat(bins.minDb());
        buffer.putFloat(bins.maxDb());
        buffer.put(bins.bins());
        return buffer.array();
    }

    public static byte[] audio(int sub, long callKey, int sequence, byte[] packet)
    {
        ByteBuffer buffer = ByteBuffer.allocate(AUDIO_HEADER + packet.length).order(ByteOrder.LITTLE_ENDIAN);
        header(buffer, KIND_AUDIO, sub);
        buffer.putLong(callKey);
        buffer.putInt(sequence);
        buffer.put(packet);
        return buffer.array();
    }

    private static void header(ByteBuffer buffer, int kind, int sub)
    {
        buffer.put((byte)VERSION);
        buffer.put((byte)kind);
        buffer.putShort((short)0);
        buffer.putInt(sub);
    }
}
