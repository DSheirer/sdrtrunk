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
package io.github.dsheirer.dsp.filter.channelizer.ols;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.buffer.FloatNativeBuffer;
import io.github.dsheirer.dsp.fft.MixedRadixComplexFFT;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Overlap-save channelizer input: the in-place receive path matches the chunked native buffer path bit for bit,
 * block timestamps advance by exactly one hop, and reset() starts the stream over.
 */
public class OverlapSaveChannelizerTest
{
    /**
     * Records a copy of each block's spectrum, index and timestamp.
     */
    private static class Recorder implements OverlapSaveChannelizer.BlockListener
    {
        final List<float[]> mSpectra = new ArrayList<>();
        final List<Long> mBlocks = new ArrayList<>();
        final List<Long> mTimestamps = new ArrayList<>();

        @Override
        public void processBlock(float[] spectrumI, float[] spectrumQ, long block, long timestamp)
        {
            float[] spectrum = Arrays.copyOf(spectrumI, spectrumI.length * 2);
            System.arraycopy(spectrumQ, 0, spectrum, spectrumI.length, spectrumQ.length);
            mSpectra.add(spectrum);
            mBlocks.add(block);
            mTimestamps.add(timestamp);
        }
    }

    private static OverlapSaveChannelizer channelizer(double sampleRate, int size, int hop, Recorder recorder)
    {
        OverlapSaveChannelizer channelizer = new OverlapSaveChannelizer(sampleRate, size, hop,
            new MixedRadixComplexFFT(size));
        channelizer.addChannel(recorder);
        return channelizer;
    }

    private static float[] noise(int samples, Random random)
    {
        float[] interleaved = new float[samples * 2];

        for(int x = 0; x < interleaved.length; x++)
        {
            interleaved[x] = random.nextFloat() * 2.0f - 1.0f;
        }

        return interleaved;
    }

    /**
     * The chunked path that the manager used for every buffer: the native buffer's interleaved iterator, which copies
     * 2048 sample chunks that all carry the buffer's timestamp.
     */
    private static void receiveChunked(OverlapSaveChannelizer channelizer, FloatNativeBuffer buffer)
    {
        Iterator<InterleavedComplexSamples> iterator = buffer.iteratorInterleaved();

        while(iterator.hasNext())
        {
            InterleavedComplexSamples samples = iterator.next();
            channelizer.receive(samples.samples(), samples.timestamp());
        }
    }

    private static void assertSameBlocks(Recorder expected, Recorder actual, String label)
    {
        assertEquals(expected.mSpectra.size(), actual.mSpectra.size(), label + " block count");
        assertEquals(expected.mBlocks, actual.mBlocks, label + " block indices");
        assertEquals(expected.mTimestamps, actual.mTimestamps, label + " timestamps");

        for(int b = 0; b < expected.mSpectra.size(); b++)
        {
            assertArrayEquals(expected.mSpectra.get(b), actual.mSpectra.get(b), label + " block " + b);
        }
    }

    /**
     * After reset(), the channelizer produces the same spectra and timestamps as a new channelizer given the same
     * samples: the partial block and the overlap from before the reset don't reach the first block after it.
     */
    @Test
    void resetDiscardsPartialBlockAndOverlap()
    {
        double sampleRate = 1000000.0;
        int size = 1000;
        int hop = 800;
        Random random = new Random(7);
        Recorder reset = new Recorder();
        Recorder fresh = new Recorder();
        OverlapSaveChannelizer resetChannelizer = channelizer(sampleRate, size, hop, reset);
        OverlapSaveChannelizer freshChannelizer = channelizer(sampleRate, size, hop, fresh);

        //Two blocks and part of a third before the restart
        resetChannelizer.receive(noise(2 * hop + 300, random), 1700000000000L);
        int before = reset.mSpectra.size();
        resetChannelizer.reset();

        float[] samples = noise(6 * hop + 100, random);
        long timestamp = 1700000005000L;
        resetChannelizer.receive(samples, timestamp);
        freshChannelizer.receive(samples, timestamp);

        assertEquals(2, before);
        assertEquals(fresh.mSpectra.size(), reset.mSpectra.size() - before);

        for(int b = 0; b < fresh.mSpectra.size(); b++)
        {
            assertArrayEquals(fresh.mSpectra.get(b), reset.mSpectra.get(before + b), "block " + b);
            assertEquals(fresh.mTimestamps.get(b), reset.mTimestamps.get(before + b), "timestamp " + b);
        }
    }

    /**
     * Random buffer lengths, mostly not multiples of the hop, and random offsets into a larger array: the in-place
     * paths (the array part and the manager's float buffer path) produce the same spectra, block indices and
     * timestamps as the chunked path.
     */
    @Test
    void inPlaceReceiveMatchesChunked()
    {
        double sampleRate = 1000000.0;
        int size = 1000;
        int hop = 800;

        for(int seed = 1; seed <= 20; seed++)
        {
            Random random = new Random(seed);
            Recorder chunked = new Recorder();
            Recorder inPlace = new Recorder();
            Recorder manager = new Recorder();
            OverlapSaveChannelizer chunkedChannelizer = channelizer(sampleRate, size, hop, chunked);
            OverlapSaveChannelizer inPlaceChannelizer = channelizer(sampleRate, size, hop, inPlace);
            OverlapSaveChannelizer managerChannelizer = channelizer(sampleRate, size, hop, manager);
            long sample = 0;
            long base = 1700000000000L + random.nextInt(1000);
            int total = hop * (15 + random.nextInt(10)) + random.nextInt(hop);

            while(sample < total)
            {
                //Lengths from 1 sample to several 2048 sample chunks, sometimes exactly a hop
                int length = random.nextInt(8) == 0 ? hop : 1 + random.nextInt(5000);
                length = (int)Math.min(length, total - sample);
                int before = random.nextInt(300);
                int after = random.nextInt(300);
                float[] padded = noise(before + length + after, random);
                float[] buffer = Arrays.copyOfRange(padded, 2 * before, 2 * (before + length));
                long timestamp = base + (long)(sample * 1000.0 / sampleRate) + random.nextInt(6);

                receiveChunked(chunkedChannelizer, new FloatNativeBuffer(buffer, timestamp, 1000.0f));
                inPlaceChannelizer.receive(padded, before, length, timestamp);
                OverlapSaveChannelManager.receive(managerChannelizer, new FloatNativeBuffer(buffer, timestamp, 1000.0f));
                sample += length;
            }

            assertTrue(chunked.mSpectra.size() >= 15, "seed " + seed + " blocks");
            assertSameBlocks(chunked, inPlace, "seed " + seed + " in place");
            assertSameBlocks(chunked, manager, "seed " + seed + " manager");
        }
    }

    /**
     * The interleaved array overload with the whole array matches the array part with offset zero.
     */
    @Test
    void wholeArrayMatchesArrayPart()
    {
        Random random = new Random(7);
        Recorder whole = new Recorder();
        Recorder part = new Recorder();
        OverlapSaveChannelizer wholeChannelizer = channelizer(1000000.0, 1000, 800, whole);
        OverlapSaveChannelizer partChannelizer = channelizer(1000000.0, 1000, 800, part);

        for(int x = 0; x < 40; x++)
        {
            float[] samples = noise(1 + random.nextInt(1500), random);
            wholeChannelizer.receive(samples, x);
            partChannelizer.receive(samples, 0, samples.length / 2, x);
        }

        assertSameBlocks(whole, part, "whole array");
    }

    @Test
    void arrayPartBoundsChecked()
    {
        OverlapSaveChannelizer channelizer = channelizer(1000000.0, 1000, 800, new Recorder());
        float[] samples = new float[200];
        assertThrows(IndexOutOfBoundsException.class, () -> channelizer.receive(samples, -1, 10, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> channelizer.receive(samples, 0, -1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> channelizer.receive(samples, 95, 6, 0));
        channelizer.receive(samples, 95, 5, 0);
        channelizer.receive(samples, 100, 0, 0);
    }

    /**
     * At the production rates each block's timestamp is exactly one hop (20 ms) after the previous block's, with
     * Airspy sized buffers whose timestamps jitter, on both the in-place and chunked paths.  The first block has the
     * first buffer's timestamp, and a break in the buffer timestamps moves the clock once.
     */
    @Test
    void hopTimestampsAdvanceByOneHop()
    {
        for(double sampleRate : new double[]{2400000.0, 2500000.0, 10000000.0})
        {
            for(boolean chunked : new boolean[]{false, true})
            {
                Recorder recorder = new Recorder();
                OverlapSaveChannelizer channelizer = OverlapSaveChannelizer.create(sampleRate);
                channelizer.addChannel(recorder);
                double hopMilliseconds = channelizer.getHop() * 1000.0 / sampleRate;
                assertEquals(20.0, hopMilliseconds, 1E-12, "hop at " + sampleRate);

                Random random = new Random(3);
                int bufferLength = 32768;
                float[] samples = noise(bufferLength, random);
                long first = 1700000000000L;
                long breakSample = (long)channelizer.getHop() * 12 + 1234;
                long breakJump = 5000;
                long sample = 0;
                int blocks = 25;

                while(sample < (long)channelizer.getHop() * blocks)
                {
                    //Timestamps at the buffer's first sample, plus up to 8 ms of delivery jitter, except the first
                    long timestamp = first + (long)(sample * 1000.0 / sampleRate) + (sample == 0 ? 0 :
                        random.nextInt(9)) + (sample >= breakSample ? breakJump : 0);
                    FloatNativeBuffer buffer = new FloatNativeBuffer(samples, timestamp, 1000.0f);

                    if(chunked)
                    {
                        receiveChunked(channelizer, buffer);
                    }
                    else
                    {
                        OverlapSaveChannelManager.receive(channelizer, buffer);
                    }

                    sample += bufferLength;
                }

                String label = sampleRate + (chunked ? " chunked" : " in place");
                assertEquals(first, (long)recorder.mTimestamps.get(0), label + " first block");
                int steps = 0;

                for(int b = 1; b < recorder.mTimestamps.size(); b++)
                {
                    long difference = recorder.mTimestamps.get(b) - recorder.mTimestamps.get(b - 1);

                    if(difference != 20)
                    {
                        steps++;
                        assertTrue(difference > breakJump - 100 && difference < breakJump + 100,
                            label + " clock step at block " + b + " was " + difference + " ms");
                    }
                }

                assertEquals(1, steps, label + " clock steps");
                assertTrue(recorder.mTimestamps.size() >= blocks - 1, label + " blocks");
            }
        }
    }
}
