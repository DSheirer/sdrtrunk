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
package io.github.dsheirer.dsp.fft;

import io.github.dsheirer.dsp.filter.channelizer.ComplexPolyphaseChannelizerM2;
import java.lang.management.ManagementFactory;
import java.util.Random;
import org.jtransforms.fft.FloatFFT_1D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Verifies that the MixedRadixComplexFFT produces the same results as JTransforms FloatFFT_1D and does not allocate
 * memory per transform.
 */
public class MixedRadixComplexFFTTest
{
    /**
     * Maximum error relative to the largest output magnitude.
     */
    private static final double RELATIVE_TOLERANCE = 2e-6;

    /**
     * Sizes covering each radix (2, 3, 4, 5), mixed factorizations, powers of two and the channelizer channel counts.
     */
    private static final int[] SIZES = {1, 2, 3, 4, 5, 6, 8, 9, 10, 12, 15, 16, 25, 27, 30, 32, 40, 45, 50, 60, 64, 72,
        80, 90, 96, 100, 120, 125, 128, 150, 160, 180, 192, 200, 240, 250, 256, 270, 300, 320, 360, 384, 400, 450, 480,
        500, 512, 600, 640, 720, 750, 768, 800, 900, 960, 1000, 1024, 1080, 1200, 1500, 2000, 2400, 3000, 4096, 6000};

    private static float[] randomSamples(int length, Random random)
    {
        float[] samples = new float[length];

        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = random.nextFloat() * 2.0f - 1.0f;
        }

        return samples;
    }

    /**
     * Asserts that the actual values match the expected values within a tolerance relative to the largest expected
     * magnitude.
     */
    private static void assertClose(float[] expected, float[] actual, String message)
    {
        assertEquals(expected.length, actual.length, message + " length");

        double maxMagnitude = 1e-30;
        double maxError = 0;

        for(int x = 0; x < expected.length; x++)
        {
            maxMagnitude = Math.max(maxMagnitude, Math.abs(expected[x]));
            maxError = Math.max(maxError, Math.abs(expected[x] - actual[x]));
        }

        double relativeError = maxError / maxMagnitude;
        assertTrue(relativeError <= RELATIVE_TOLERANCE, message + " relative error " + relativeError +
            " exceeds " + RELATIVE_TOLERANCE);
    }

    /**
     * Double precision brute force DFT reference.
     * @param samples interleaved complex
     * @param sign -1 forward, +1 inverse
     */
    private static float[] referenceDFT(float[] samples, int sign)
    {
        int n = samples.length / 2;
        float[] result = new float[samples.length];

        for(int k = 0; k < n; k++)
        {
            double re = 0;
            double im = 0;

            for(int t = 0; t < n; t++)
            {
                double angle = sign * 2.0 * Math.PI * ((long)k * t % n) / n;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                re += samples[2 * t] * cos - samples[2 * t + 1] * sin;
                im += samples[2 * t] * sin + samples[2 * t + 1] * cos;
            }

            result[2 * k] = (float)re;
            result[2 * k + 1] = (float)im;
        }

        return result;
    }

    @Test
    public void forwardMatchesJTransforms()
    {
        Random random = new Random(1);

        for(int size : SIZES)
        {
            float[] input = randomSamples(2 * size, random);
            float[] expected = input.clone();
            new FloatFFT_1D(size).complexForward(expected);
            float[] actual = input.clone();
            new MixedRadixComplexFFT(size).forward(actual);
            assertClose(expected, actual, "Forward size " + size);
        }
    }

    @Test
    public void inverseMatchesJTransforms()
    {
        Random random = new Random(2);

        for(int size : SIZES)
        {
            for(boolean scale : new boolean[]{true, false})
            {
                float[] input = randomSamples(2 * size, random);
                float[] expected = input.clone();
                new FloatFFT_1D(size).complexInverse(expected, scale);
                float[] actual = input.clone();
                new MixedRadixComplexFFT(size).inverse(actual, scale);
                assertClose(expected, actual, "Inverse size " + size + " scale " + scale);
            }
        }
    }

    @Test
    public void matchesDoublePrecisionReference()
    {
        Random random = new Random(3);

        for(int size : SIZES)
        {
            if(size > 1200)
            {
                continue; //Brute force reference is O(N^2)
            }

            MixedRadixComplexFFT fft = new MixedRadixComplexFFT(size);
            float[] input = randomSamples(2 * size, random);

            float[] forward = input.clone();
            fft.forward(forward);
            assertClose(referenceDFT(input, -1), forward, "Forward reference size " + size);

            float[] inverse = input.clone();
            fft.inverse(inverse, false);
            assertClose(referenceDFT(input, 1), inverse, "Inverse reference size " + size);
        }
    }

    /**
     * Mirrors the channelizer usage: repeated in-place inverse transforms of consecutive blocks in a larger array,
     * reusing a single instance and its scratch buffer.  Also verifies that data outside each block is not modified.
     */
    @Test
    public void offsetBlocksMatchJTransforms()
    {
        Random random = new Random(4);
        int blocks = 7;

        for(int size : new int[]{2, 72, 96, 100, 128, 240, 400, 480, 600})
        {
            int blockLength = 2 * size;
            int guard = 3;
            float[] input = randomSamples(blocks * blockLength + 2 * guard, random);

            float[] expected = input.clone();
            FloatFFT_1D jtransforms = new FloatFFT_1D(size);
            float[] actual = input.clone();
            MixedRadixComplexFFT fft = new MixedRadixComplexFFT(size);

            for(int offset = guard; offset < guard + blocks * blockLength; offset += blockLength)
            {
                jtransforms.complexInverse(expected, offset, true);
                fft.inverse(actual, offset, true);
            }

            assertClose(expected, actual, "Offset blocks size " + size);

            for(int x = 0; x < guard; x++)
            {
                assertEquals(input[x], actual[x], "Leading guard modified size " + size);
                assertEquals(input[input.length - 1 - x], actual[actual.length - 1 - x],
                    "Trailing guard modified size " + size);
            }
        }
    }

    @Test
    public void roundTripRestoresInput()
    {
        Random random = new Random(5);

        for(int size : SIZES)
        {
            MixedRadixComplexFFT fft = new MixedRadixComplexFFT(size);
            float[] input = randomSamples(2 * size, random);
            float[] samples = input.clone();
            fft.forward(samples);
            fft.inverse(samples, true);
            assertArrayEquals(input, samples, 1e-5f, "Round trip size " + size);
        }
    }

    @Test
    public void supportsAllChannelizerChannelCounts()
    {
        for(double sampleRate = 50_000; sampleRate <= 61_440_000; sampleRate += 25_000)
        {
            int channelCount = ComplexPolyphaseChannelizerM2.getChannelCount(sampleRate);
            assertTrue(MixedRadixComplexFFT.isSupported(channelCount), "Channel count " + channelCount +
                " for sample rate " + sampleRate);
        }
    }

    @Test
    public void rejectsUnsupportedSizes()
    {
        for(int size : new int[]{0, -4, 7, 14, 49, 244, 1001})
        {
            assertFalse(MixedRadixComplexFFT.isSupported(size), "Size " + size);
            assertThrows(IllegalArgumentException.class, () -> new MixedRadixComplexFFT(size), "Size " + size);
        }
    }

    /**
     * Verifies that repeated transforms do not allocate memory, which is the reason this class exists.
     */
    @Test
    public void doesNotAllocatePerTransform()
    {
        assumeTrue(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean threadMXBean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        assumeTrue(threadMXBean.isThreadAllocatedMemorySupported() && threadMXBean.isThreadAllocatedMemoryEnabled());

        int size = 400;
        int iterations = 10_000;
        MixedRadixComplexFFT fft = new MixedRadixComplexFFT(size);
        float[] samples = randomSamples(2 * size, new Random(6));

        //Warm up so that class loading and JIT compilation are complete before measuring
        for(int x = 0; x < iterations; x++)
        {
            fft.inverse(samples, 0, true);
        }

        long threadId = Thread.currentThread().threadId();
        long before = threadMXBean.getThreadAllocatedBytes(threadId);

        for(int x = 0; x < iterations; x++)
        {
            fft.inverse(samples, 0, true);
        }

        long allocated = threadMXBean.getThreadAllocatedBytes(threadId) - before;

        //JTransforms allocates 2N floats (3200 bytes at N=400) per call, about 32 MB for these iterations
        assertTrue(allocated < 64 * 1024, "Allocated " + allocated + " bytes over " + iterations + " transforms");
    }
}
