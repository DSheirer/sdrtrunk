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

package io.github.dsheirer.buffer;

import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests signed and unsigned byte sample conversion, DC correction, sample fragmentation, and input validation.
 */
class ByteNativeBuffersTest
{
    @Test
    void unsignedFactoryCopiesSamplesAndAppliesCombinedDcCorrection()
    {
        int fragmentSize = 8192;
        byte[] raw = new byte[fragmentSize * 4];
        Arrays.fill(raw, 0, fragmentSize * 2, (byte)255);
        ByteNativeBufferFactory factory = new ByteNativeBufferFactory();
        factory.setSamplesPerMillisecond(1024);

        INativeBuffer buffer = factory.getBuffer(ByteBuffer.wrap(raw), 100);
        Arrays.fill(raw, (byte)127);

        // The two equal-sized fragments contain opposite offsets, so their combined DC is zero.
        float maximum = 127.5f / 128;
        assertFragments(buffer, fragmentSize, 108, maximum, maximum, -maximum, -maximum);

        Arrays.fill(raw, (byte)255);
        INativeBuffer biased = factory.getBuffer(ByteBuffer.wrap(raw), 100);
        assertEquals(maximum * 0.95f, biased.iterator().next().i()[0], 0.000001f);
    }

    @Test
    void signedFactoryAppliesIndependentDcCorrectionToEachChannel()
    {
        int fragmentSize = 2048;
        byte[] raw = new byte[fragmentSize * 4];

        for(int index = 0; index < raw.length; index += 2)
        {
            raw[index] = (byte)(index < fragmentSize * 2 ? -128 : -64);
            raw[index + 1] = (byte)(index < fragmentSize * 2 ? 64 : 32);
        }

        SignedByteNativeBufferFactory factory = new SignedByteNativeBufferFactory();
        factory.setSamplesPerMillisecond(512);
        INativeBuffer buffer = factory.getBuffer(ByteBuffer.wrap(raw), 100);
        Arrays.fill(raw, (byte)0);

        assertFragments(buffer, fragmentSize, 104, -0.9625f, 0.48125f, -0.4625f, 0.23125f);
    }

    @Test
    void signedFactoryPreservesFractionalDcAverages()
    {
        byte[] raw = new byte[4096];

        // Alternate (0, 0) with (1, -3) to produce signed byte means of 0.5 and -1.5.
        for(int index = 2; index < raw.length; index += 4)
        {
            raw[index] = 1;
            raw[index + 1] = -3;
        }

        SignedByteNativeBufferFactory factory = new SignedByteNativeBufferFactory();
        ComplexSamples corrected = factory.getBuffer(ByteBuffer.wrap(raw), 0).iterator().next();

        // The first input pair is zero, so its output is the negative of the DC correction.
        assertEquals(-0.5f / 128.0f * 0.05f, corrected.i()[0], 1e-7f, "I correction");
        assertEquals(1.5f / 128.0f * 0.05f, corrected.q()[0], 1e-7f, "Q correction");
    }

    @Test
    void rejectsUnalignedByteArrays()
    {
        assertThrows(IllegalArgumentException.class, () -> new ByteNativeBuffer(new byte[3], 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new SignedByteNativeBuffer(new byte[3], 0, 0, 0, 1));
    }

    @Test
    void poisonPillCarriesNoSamples()
    {
        INativeBuffer buffer = new NativeBufferPoisonPill();
        assertEquals(0, buffer.sampleCount());
        assertEquals(0, buffer.getTimestamp());
        assertNull(buffer.iterator());
        assertNull(buffer.iteratorInterleaved());
    }

    private static void assertFragments(INativeBuffer buffer, int fragmentSize, long secondTimestamp,
                                        float firstI, float firstQ, float secondI, float secondQ)
    {
        assertEquals(fragmentSize * 2, buffer.sampleCount());
        assertEquals(100, buffer.getTimestamp());
        Iterator<ComplexSamples> split = buffer.iterator();
        Iterator<InterleavedComplexSamples> interleaved = buffer.iteratorInterleaved();

        assertFragment(split, interleaved, fragmentSize, 100, firstI, firstQ);
        assertFragment(split, interleaved, fragmentSize, secondTimestamp, secondI, secondQ);

        assertFalse(split.hasNext());
        assertFalse(interleaved.hasNext());
    }

    private static void assertFragment(Iterator<ComplexSamples> split, Iterator<InterleavedComplexSamples> interleaved,
                                       int size, long timestamp, float expectedI, float expectedQ)
    {
        assertTrue(split.hasNext());
        assertTrue(interleaved.hasNext());
        ComplexSamples channels = split.next();
        InterleavedComplexSamples pairs = interleaved.next();
        assertEquals(timestamp, channels.timestamp());
        assertEquals(timestamp, pairs.timestamp());
        assertEquals(size, channels.i().length);
        assertEquals(size, channels.q().length);
        assertEquals(size * 2, pairs.samples().length);

        for(int index = 0; index < size; index++)
        {
            assertEquals(expectedI, channels.i()[index], 0.000001f);
            assertEquals(expectedQ, channels.q()[index], 0.000001f);
            assertEquals(expectedI, pairs.samples()[index * 2], 0.000001f);
            assertEquals(expectedQ, pairs.samples()[index * 2 + 1], 0.000001f);
        }
    }
}
