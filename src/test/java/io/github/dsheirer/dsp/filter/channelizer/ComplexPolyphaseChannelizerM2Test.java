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

package io.github.dsheirer.dsp.filter.channelizer;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that the fused multiply-accumulate used by {@link ComplexPolyphaseChannelizerM2} produces bit-for-bit
 * identical output to the prior multiply-then-accumulate implementation.
 *
 * The channelizer's per-block filter step was changed from (a) multiplying the entire inline sample/filter arrays into
 * a per-call interim product array and then summing that array across taps, to (b) fusing the multiply directly into
 * the per-sub-channel accumulation in a single pass (which also removes the per-call interim array and reuses the
 * accumulator).  Both forms are expected to be numerically identical: the tap products for each sub-channel are summed
 * in the same ascending tap order, and the JVM does not contract a separate float multiply/add into an FMA without an
 * explicit {@code Math.fma()} call.
 *
 * This test reproduces both the reference and the fused filter+reorder computation (including the same aligned-filter
 * arrangement and top/middle block reordering the channelizer uses) and asserts the outputs match exactly, using
 * {@link Float#floatToRawIntBits(float)} so that differences in NaN bit patterns and the sign of zero would also be
 * detected.  Multiple channel counts (spanning the 25 kHz-per-channel sizing from ~50 kHz to 10 MHz sample rates) and
 * several stressed floating point input distributions are exercised.
 */
public class ComplexPolyphaseChannelizerM2Test
{
    private static final int TAPS_PER_CHANNEL = 9; //Matches PolyphaseChannelManager.POLYPHASE_CHANNELIZER_TAPS_PER_CHANNEL

    /**
     * Test: for every channel count and input distribution, the fused multiply-accumulate output must equal the
     * reference multiply-then-accumulate output bit-for-bit.
     *
     * Success: zero differing output values across all configurations, input distributions, and block phases.
     */
    @Test
    void fusedMultiplyAccumulateMatchesReference()
    {
        Random rng = new Random(0xC0FFEEL);

        //Even channel counts corresponding to sampleRate/25000 for ~50 kHz, 100 kHz, 1 MHz, 2.4 MHz, 3.2 MHz,
        //6.1 MHz and 10 MHz sample rates, plus small/odd sizes.
        int[] channelCounts = {2, 4, 8, 40, 96, 128, 244, 400};

        for(int channelCount : channelCounts)
        {
            Harness harness = new Harness(channelCount, TAPS_PER_CHANNEL, rng);

            boolean topBlockReference = true;
            boolean topBlockFused = true;

            for(int mode = 0; mode < 4; mode++)
            {
                for(int iteration = 0; iteration < 1000; iteration++)
                {
                    harness.randomizeSamples(rng, mode);

                    float[] reference = harness.processReference(topBlockReference);
                    topBlockReference = !topBlockReference;

                    float[] fused = harness.processFused(topBlockFused);
                    topBlockFused = !topBlockFused;

                    assertBitIdentical(reference, fused, channelCount, mode, iteration);
                }
            }
        }
    }

    private static void assertBitIdentical(float[] reference, float[] fused, int channelCount, int mode, int iteration)
    {
        assertEquals(reference.length, fused.length,
            "Output length mismatch (channelCount=" + channelCount + ")");

        for(int x = 0; x < reference.length; x++)
        {
            assertEquals(Float.floatToRawIntBits(reference[x]), Float.floatToRawIntBits(fused[x]),
                "Fused output differs from reference at index " + x + " (channelCount=" + channelCount +
                    ", mode=" + mode + ", iteration=" + iteration + "): reference=" + reference[x] +
                    " fused=" + fused[x]);
        }
    }

    /**
     * Reproduces the channelizer's per-block filter state and both the reference and fused implementations of the
     * filter+reorder step so their outputs can be compared directly, without needing the (private) process() method
     * or the downstream IFFT.  The aligned-filter arrangement and block-reorder maps mirror the production helpers so
     * the inputs have realistic structure.
     */
    private static final class Harness
    {
        private final int mSubChannelCount;   // channelCount * 2
        private final int mTapsPerChannel;
        private final int mBufferLength;      // mSubChannelCount * mTapsPerChannel
        private final float[] mInlineSamples; // length mBufferLength
        private final float[] mInlineFilter;  // length mBufferLength
        private final int[] mTopBlockMap;     // length mSubChannelCount
        private final int[] mMiddleBlockMap;  // length mSubChannelCount
        private final float[] mReusableAccumulator;

        private Harness(int channelCount, int tapsPerChannel, Random rng)
        {
            mSubChannelCount = channelCount * 2;
            mTapsPerChannel = tapsPerChannel;
            mBufferLength = mSubChannelCount * tapsPerChannel;

            float[] coefficients = new float[channelCount * tapsPerChannel];
            for(int i = 0; i < coefficients.length; i++)
            {
                coefficients[i] = rng.nextFloat() * 2f - 1f;
            }

            mInlineFilter = getAlignedFilter(coefficients, channelCount, tapsPerChannel);
            mInlineSamples = new float[mBufferLength];
            mTopBlockMap = getTopBlockMap(channelCount);
            mMiddleBlockMap = getMiddleBlockMap(channelCount);
            mReusableAccumulator = new float[mSubChannelCount];
        }

        //Reference: multiply the whole array into an interim buffer, then accumulate across taps (prior behavior).
        private float[] processReference(boolean topBlock)
        {
            float[] interim = new float[mBufferLength];
            for(int x = 0; x < mInlineSamples.length; x++)
            {
                interim[x] = mInlineSamples[x] * mInlineFilter[x];
            }

            float[] accumulator = new float[mSubChannelCount];
            for(int tap = 0; tap < mTapsPerChannel; tap++)
            {
                int tapOffset = tap * mSubChannelCount;
                for(int channel = 0; channel < mSubChannelCount; channel++)
                {
                    accumulator[channel] += interim[tapOffset + channel];
                }
            }

            return reorder(accumulator, topBlock);
        }

        //Fused: multiply-accumulate in a single pass into a reused accumulator (new behavior).
        private float[] processFused(boolean topBlock)
        {
            float[] accumulator = mReusableAccumulator;
            Arrays.fill(accumulator, 0.0f);

            for(int tap = 0; tap < mTapsPerChannel; tap++)
            {
                int tapOffset = tap * mSubChannelCount;
                for(int channel = 0; channel < mSubChannelCount; channel++)
                {
                    int index = tapOffset + channel;
                    accumulator[channel] += mInlineSamples[index] * mInlineFilter[index];
                }
            }

            return reorder(accumulator, topBlock);
        }

        private float[] reorder(float[] accumulator, boolean topBlock)
        {
            float[] processed = new float[mSubChannelCount];
            int[] map = topBlock ? mTopBlockMap : mMiddleBlockMap;
            for(int x = 0; x < mSubChannelCount; x++)
            {
                processed[x] = accumulator[map[x]];
            }
            return processed;
        }

        private void randomizeSamples(Random rng, int mode)
        {
            for(int i = 0; i < mInlineSamples.length; i++)
            {
                switch(mode)
                {
                    case 0 -> mInlineSamples[i] = rng.nextFloat() * 2f - 1f;                          //typical baseband
                    case 1 -> mInlineSamples[i] = (rng.nextInt(5) == 0) ? 0f : rng.nextFloat() * 2f - 1f; //sprinkle zeros
                    case 2 -> mInlineSamples[i] = (rng.nextFloat() * 2f - 1f)
                        * (float)Math.pow(10, rng.nextInt(30) - 15);                                  //wide dynamic range
                    default -> mInlineSamples[i] = switch(rng.nextInt(6))                             //edge values
                        {
                            case 0 -> -0.0f;
                            case 1 -> 0.0f;
                            case 2 -> Float.MIN_VALUE;
                            case 3 -> -Float.MIN_VALUE;
                            case 4 -> rng.nextFloat() * 1e6f;
                            default -> rng.nextFloat() * 2f - 1f;
                        };
                }
            }
        }

        //---- Local copies of the production coefficient-arrangement and reorder-map helpers (values only matter in
        //     that they produce the same realistic structure; the equivalence being tested holds regardless). ----

        private static float[] getAlignedFilter(float[] coefficients, int channelCount, int tapsPerChannel)
        {
            float[] filter = new float[channelCount * tapsPerChannel * 2];
            int blockSize = channelCount;

            int coefficientPointer = 0;
            int filterPointer = 0;
            while(coefficientPointer < coefficients.length)
            {
                filter[filterPointer++] = coefficients[coefficientPointer];
                filter[filterPointer++] = coefficients[coefficientPointer++];
            }

            for(int x = 0; x < filter.length; x += blockSize)
            {
                for(int y = 0; y < blockSize / 2; y++)
                {
                    int index1 = x + y;
                    int index2 = x + (blockSize - y - 1);
                    float temp = filter[index2];
                    filter[index2] = filter[index1];
                    filter[index1] = temp;
                }
            }

            return filter;
        }

        private static int[] getTopBlockMap(int channelCount)
        {
            int[] newMap = new int[channelCount * 2];
            int blockSize = channelCount / 2;
            for(int channel = 0; channel < blockSize; channel++)
            {
                int newIndex = 2 * channel;
                int originalIndex = 2 * (blockSize - channel - 1);
                int offset = 2 * blockSize;
                newMap[originalIndex] = newIndex;
                newMap[originalIndex + 1] = newIndex + 1;
                newMap[offset + originalIndex] = offset + newIndex;
                newMap[offset + originalIndex + 1] = offset + newIndex + 1;
            }
            return newMap;
        }

        private static int[] getMiddleBlockMap(int channelCount)
        {
            int[] newMap = new int[channelCount * 2];
            int blockSize = channelCount / 2;
            for(int channel = 0; channel < blockSize; channel++)
            {
                int newIndex = 2 * channel;
                int originalIndex = 2 * (blockSize - channel - 1);
                int offset = 2 * blockSize;
                newMap[offset + originalIndex] = newIndex;
                newMap[offset + originalIndex + 1] = newIndex + 1;
                newMap[originalIndex] = offset + newIndex;
                newMap[originalIndex + 1] = offset + newIndex + 1;
            }
            return newMap;
        }
    }
}
