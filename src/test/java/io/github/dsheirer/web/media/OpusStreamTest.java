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

import io.github.jaredmdobson.concentus.OpusDecoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opus encoding of call audio and the binary frame layouts.
 */
public class OpusStreamTest
{
    private static float[] tone(double frequency, int samples, float amplitude)
    {
        float[] tone = new float[samples];

        for(int x = 0; x < samples; x++)
        {
            tone[x] = amplitude * (float)Math.sin(2 * Math.PI * frequency * x / OpusStream.SAMPLE_RATE);
        }

        return tone;
    }

    /**
     * Power of one frequency in the samples (Goertzel)
     */
    private static double power(short[] samples, int offset, int length, double frequency)
    {
        double coefficient = 2 * Math.cos(2 * Math.PI * frequency / OpusStream.SAMPLE_RATE);
        double s1 = 0;
        double s2 = 0;

        for(int x = offset; x < offset + length; x++)
        {
            double s = samples[x] + coefficient * s1 - s2;
            s2 = s1;
            s1 = s;
        }

        return s1 * s1 + s2 * s2 - coefficient * s1 * s2;
    }

    @Test
    void encodesDecodableVoiceBandAudio() throws Exception
    {
        OpusStream stream = new OpusStream();
        List<byte[]> packets = new ArrayList<>();

        //One second in odd sized buffers, as the decoders produce them
        float[] audio = tone(1000, 8000, 0.5f);

        for(int offset = 0; offset < audio.length; offset += 230)
        {
            float[] buffer = new float[Math.min(230, audio.length - offset)];
            System.arraycopy(audio, offset, buffer, 0, buffer.length);
            stream.encode(buffer, packets::add);
        }

        stream.flush(packets::add);
        assertEquals(50, packets.size(), "one packet per 20 ms");

        int bytes = packets.stream().mapToInt(packet -> packet.length).sum();
        assertTrue(bytes < 16_000 / 8 * 1.5, "about 16 kbps: " + bytes + " bytes per second");

        OpusDecoder decoder = new OpusDecoder(OpusStream.SAMPLE_RATE, 1);
        short[] decoded = new short[8000];
        int position = 0;

        for(byte[] packet: packets)
        {
            position += decoder.decode(packet, 0, packet.length, decoded, position, OpusStream.FRAME_SAMPLES, false);
        }

        assertEquals(8000, position);

        //After the codec settles, the 1 kHz tone dominates a neighboring frequency and keeps its level
        double atTone = power(decoded, 4000, 2000, 1000);
        double offTone = power(decoded, 4000, 2000, 1500);
        assertTrue(atTone > offTone * 100, "tone " + atTone + " off " + offTone);

        double rms = 0;

        for(int x = 4000; x < 6000; x++)
        {
            rms += decoded[x] * (double)decoded[x];
        }

        rms = Math.sqrt(rms / 2000) / Short.MAX_VALUE;
        double expected = 0.5 / Math.sqrt(2);
        assertTrue(Math.abs(20 * Math.log10(rms / expected)) < 3, "level within 3 dB: " + rms);
    }

    @Test
    void partialFrameIsPaddedOnFlush()
    {
        OpusStream stream = new OpusStream();
        List<byte[]> packets = new ArrayList<>();
        stream.encode(new float[100], packets::add);
        assertTrue(packets.isEmpty());
        stream.flush(packets::add);
        assertEquals(1, packets.size());
        stream.flush(packets::add);
        assertEquals(1, packets.size(), "nothing left to flush");
    }

    @Test
    void binaryFrameLayouts()
    {
        BinaryFrames.QuantizedBins bins = BinaryFrames.QuantizedBins.of(new float[]{-120, -60, -20});
        assertEquals(-120f, bins.minDb());
        assertEquals(-20f, bins.maxDb());
        assertEquals(0, bins.bins()[0] & 0xFF);
        assertEquals(153, bins.bins()[1] & 0xFF);
        assertEquals(255, bins.bins()[2] & 0xFF);

        ByteBuffer spectrum = ByteBuffer.wrap(BinaryFrames.spectrum(7, 123L, 853_000_000L, 10_000_000L, bins))
            .order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(BinaryFrames.SPECTRUM_HEADER + 3, spectrum.capacity());
        assertEquals(1, spectrum.get(0));
        assertEquals(BinaryFrames.KIND_SPECTRUM, spectrum.get(1));
        assertEquals(7, spectrum.getInt(4));
        assertEquals(123L, spectrum.getLong(8));
        assertEquals(853_000_000L, spectrum.getLong(16));
        assertEquals(10_000_000, spectrum.getInt(24));
        assertEquals(3, spectrum.getInt(28));
        assertEquals(-120f, spectrum.getFloat(32));
        assertEquals(-20f, spectrum.getFloat(36));

        ByteBuffer audio = ByteBuffer.wrap(BinaryFrames.audio(9, 42L, 5, new byte[]{1, 2, 3}))
            .order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(BinaryFrames.KIND_AUDIO, audio.get(1));
        assertEquals(9, audio.getInt(4));
        assertEquals(42L, audio.getLong(8));
        assertEquals(5, audio.getInt(16));
        assertEquals(3, audio.get(BinaryFrames.AUDIO_HEADER + 2));
    }
}
