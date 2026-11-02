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
package org.jtransforms.fft;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.management.ManagementFactory;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Tests that the reusable inverse FFT is an exact replacement for FloatFFT_1D.
 */
class ReusableFloatInverseFFTTest
{
    private static final int OFFSET = 6;
    private static final int LARGEST_SIZE = 5000;

    private static float[] noise(int length, Random random)
    {
        float[] samples = new float[length];

        for(int x = 0; x < length; x++)
        {
            samples[x] = random.nextFloat() * 2.0f - 1.0f;
        }

        return samples;
    }

    /**
     * Test: output values are bit-identical to FloatFFT_1D for every size, including powers of two and sizes with other
     * prime factors, and samples outside of the block aren't changed.  Each instance is used twice to show that
     * scratch contents don't carry over between calls.
     */
    @Test
    void matchesFloatFFT1D()
    {
        Random random = new Random(0xC0FFEEL);

        for(int size = 1; size <= LARGEST_SIZE; size += size < 512 ? 1 : 53)
        {
            ReusableFloatInverseFFT reusable = new ReusableFloatInverseFFT(size);
            FloatFFT_1D reference = new FloatFFT_1D(size);

            for(int pass = 0; pass < 2; pass++)
            {
                float[] expected = noise(2 * size + 2 * OFFSET, random);
                float[] actual = expected.clone();
                reference.complexInverse(expected, OFFSET, true);
                reusable.complexInverse(actual, OFFSET);
                assertArrayEquals(expected, actual, 0.0f, "Size " + size);
            }
        }
    }

    /**
     * Test: every channel count from 2 to 4800 with only factors of 2, 3 and 5 matches FloatFFT_1D.  These are the only
     * sizes the channelizer can select.
     */
    @Test
    void matchesFloatFFT1DForChannelizerSizes()
    {
        Random random = new Random(0xBEEFL);

        for(int size = 2; size <= 4800; size += 2)
        {
            int remaining = size;

            for(int factor : new int[]{2, 3, 5})
            {
                while(remaining % factor == 0)
                {
                    remaining /= factor;
                }
            }

            if(remaining == 1)
            {
                float[] expected = noise(2 * size, random);
                float[] actual = expected.clone();
                new FloatFFT_1D(size).complexInverse(expected, 0, true);
                new ReusableFloatInverseFFT(size).complexInverse(actual, 0);
                assertArrayEquals(expected, actual, 0.0f, "Size " + size);
            }
        }
    }

    /**
     * Test: transforms don't allocate, for a mixed-radix size with an odd pass count and an even pass count.
     */
    @Test
    void doesNotAllocate()
    {
        com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        long threadId = Thread.currentThread().threadId();

        for(int size : new int[]{96, 400})
        {
            ReusableFloatInverseFFT fft = new ReusableFloatInverseFFT(size);
            float[] samples = noise(2 * size, new Random(1L));
            fft.complexInverse(samples, 0);

            long before = threads.getThreadAllocatedBytes(threadId);

            for(int x = 0; x < 1000; x++)
            {
                fft.complexInverse(samples, 0);
            }

            long allocated = threads.getThreadAllocatedBytes(threadId) - before;

            //FloatFFT_1D would allocate at least 8 bytes per channel, per call.  Allow for measurement overhead.
            assertEquals(0, allocated / (1000L * size), "Size " + size + " allocated " + allocated + " bytes");
        }
    }
}
