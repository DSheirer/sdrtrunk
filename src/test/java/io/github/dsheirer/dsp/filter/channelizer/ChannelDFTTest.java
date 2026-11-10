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

import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jtransforms.fft.FloatFFT_1D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the channel DFT produces the same channel values as the IFFT, both directly and through the threaded
 * channelizer, including across channel changes.
 */
public class ChannelDFTTest
{
    private static final float TOLERANCE = 0.00001f;
    private static final int BLOCKS_PER_BATCH = 1024;
    private static final int TAPS_PER_CHANNEL = 9;
    private static final int[] CHANNEL_COUNTS = {2, 8, 72, 96, 100, 128, 400};

    private static float[] randomSamples(int length, Random random)
    {
        float[] samples = new float[length];

        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = random.nextFloat() * 2.0f - 1.0f;
        }

        return samples;
    }

    private static void assertMatchesIFFT(ChannelDFT channelDFT, int channelCount, int[] channels, Random random)
    {
        int blocks = 3;
        int blockLength = channelCount * 2;
        float[] input = randomSamples(blocks * blockLength, random);
        float[] expected = input.clone();
        FloatFFT_1D fft = new FloatFFT_1D(channelCount);

        for(int block = 0; block < blocks; block++)
        {
            fft.complexInverse(expected, block * blockLength, true);
        }

        float[] actual = input.clone();
        channelDFT.process(actual, channels);

        for(int block = 0; block < blocks; block++)
        {
            for(int channel : channels)
            {
                int index = block * blockLength + channel * 2;
                assertEquals(expected[index], actual[index], TOLERANCE, "I channel " + channel + " of " + channelCount);
                assertEquals(expected[index + 1], actual[index + 1], TOLERANCE, "Q channel " + channel + " of " + channelCount);
            }
        }
    }

    /**
     * Test: both implementations match the IFFT for each single channel and for sets of channels.
     */
    @Test
    void implementationsMatchIFFT()
    {
        Random random = new Random(0xC0FFEEL);

        for(int channelCount : CHANNEL_COUNTS)
        {
            for(ChannelDFT channelDFT : List.of(new ScalarChannelDFT(channelCount), new VectorChannelDFT(channelCount)))
            {
                for(int channel = 0; channel < channelCount; channel++)
                {
                    assertMatchesIFFT(channelDFT, channelCount, new int[]{channel}, random);
                }

                //All channels at once verifies that results don't overwrite the input of the other channels
                int[] all = new int[channelCount];
                Arrays.setAll(all, x -> x);
                assertMatchesIFFT(channelDFT, channelCount, all, random);
            }
        }
    }

    /**
     * Test: each implementation limits use to channel counts that it can process faster than the IFFT.
     */
    @Test
    void maximumChannelsAreAboveZero()
    {
        assertTrue(new ScalarChannelDFT(96).getMaximumChannels() > 0);
        assertTrue(new VectorChannelDFT(96).getMaximumChannels() > 0);
    }

    /**
     * Test: channels in use receive the same values as the IFFT through the threaded channelizer, on one index and on
     * two adjacent indexes.  Channelizer outputs include the alternating block phase handling, so this also verifies
     * that both transforms see the same block order.
     */
    @Test
    void channelizerMatchesIFFTForChannelsInUse() throws Exception
    {
        int channelCount = 96;
        float[] taps = taps(channelCount);
        Random random = new Random(1234);

        for(long frequency : new long[]{100_125_000L, 100_137_500L})
        {
            ChannelCalculator calculator = new ChannelCalculator(2_400_000.0, channelCount, 100_000_000L, 2.0);
            List<float[]> sparse = new CopyOnWriteArrayList<>();
            List<float[]> full = new CopyOnWriteArrayList<>();
            ComplexPolyphaseChannelizerM2 sparseChannelizer = channelizer(taps, channelCount);
            ComplexPolyphaseChannelizerM2 fullChannelizer = channelizer(taps, channelCount);
            CapturingSource source = new CapturingSource(frequency, calculator, sparse);
            sparseChannelizer.addChannel(source);
            fullChannelizer.addChannel(new CapturingSource(frequency, calculator, full, channelCount));
            sparseChannelizer.start();
            fullChannelizer.start();

            float[] samples = randomSamples(BLOCKS_PER_BATCH * channelCount * 3, random);
            sparseChannelizer.receive(new InterleavedComplexSamples(samples, 0));
            fullChannelizer.receive(new InterleavedComplexSamples(samples, 0));
            await(sparse, 3);
            await(full, 3);
            sparseChannelizer.stop();
            fullChannelizer.stop();

            assertChannelsMatch(sparse, full, source.getOutputProcessorIndexes(), channelCount);
        }
    }

    /**
     * Test: when an update moves a channel to a different index, the first batch after the update has valid results for
     * the new index.
     */
    @Test
    void channelUpdateAppliesBeforeTransform() throws Exception
    {
        int channelCount = 96;
        float[] taps = taps(channelCount);
        long frequency = 100_125_000L;
        List<float[]> sparse = new CopyOnWriteArrayList<>();
        List<float[]> full = new CopyOnWriteArrayList<>();
        ComplexPolyphaseChannelizerM2 sparseChannelizer = channelizer(taps, channelCount);
        ComplexPolyphaseChannelizerM2 fullChannelizer = channelizer(taps, channelCount);
        ChannelCalculator calculator = new ChannelCalculator(2_400_000.0, channelCount, 100_000_000L, 2.0);
        CapturingSource source = new CapturingSource(frequency, calculator, sparse);
        sparseChannelizer.addChannel(source);
        fullChannelizer.addChannel(new CapturingSource(frequency, calculator, full, channelCount));
        sparseChannelizer.start();
        fullChannelizer.start();

        ChannelCalculator retuned = new ChannelCalculator(2_400_000.0, channelCount, 100_050_000L, 2.0);
        source.updateOutputProcessor(retuned, new SynthesisFilterManager());

        float[] samples = randomSamples(BLOCKS_PER_BATCH * channelCount, new Random(5678));
        sparseChannelizer.receive(new InterleavedComplexSamples(samples, 0));
        fullChannelizer.receive(new InterleavedComplexSamples(samples, 0));
        await(sparse, 1);
        await(full, 1);
        sparseChannelizer.stop();
        fullChannelizer.stop();

        assertEquals(List.of(3), source.getOutputProcessorIndexes());
        assertChannelsMatch(sparse, full, source.getOutputProcessorIndexes(), channelCount);
    }

    private static void assertChannelsMatch(List<float[]> actual, List<float[]> expected, List<Integer> indexes, int channelCount)
    {
        int blockLength = channelCount * 2;

        for(int batch = 0; batch < expected.size(); batch++)
        {
            for(int index : indexes)
            {
                float[] expectedChannel = extract(expected.get(batch), index, blockLength);
                float[] actualChannel = extract(actual.get(batch), index, blockLength);
                assertArrayEquals(expectedChannel, actualChannel, TOLERANCE, "Channel " + index + " batch " + batch);
            }
        }
    }

    private static float[] extract(float[] samples, int index, int blockLength)
    {
        float[] channel = new float[samples.length / blockLength * 2];

        for(int block = 0; block < channel.length / 2; block++)
        {
            channel[block * 2] = samples[block * blockLength + index * 2];
            channel[block * 2 + 1] = samples[block * blockLength + index * 2 + 1];
        }

        return channel;
    }

    private static float[] taps(int channelCount)
    {
        float[] taps = randomSamples(channelCount * TAPS_PER_CHANNEL, new Random(99));

        for(int x = 0; x < taps.length; x++)
        {
            taps[x] /= channelCount;
        }

        return taps;
    }

    private static ComplexPolyphaseChannelizerM2 channelizer(float[] taps, int channelCount)
    {
        return new ComplexPolyphaseChannelizerM2(taps, 2_400_000, channelCount);
    }

    private static void await(List<float[]> list, int size) throws Exception
    {
        long timeout = System.currentTimeMillis() + 5000;

        while(list.size() < size && System.currentTimeMillis() < timeout)
        {
            Thread.sleep(2);
        }

        assertEquals(size, list.size(), "Batches received");
    }

    /**
     * Channel source that captures a copy of each batch.  A source can also claim more indexes than its channel, which
     * forces the channelizer to use the IFFT.
     */
    private static class CapturingSource extends PolyphaseChannelSource
    {
        private final List<float[]> mCaptured;
        private final List<Integer> mForcedIndexes;

        CapturingSource(long frequency, ChannelCalculator calculator, List<float[]> captured)
        {
            this(frequency, calculator, captured, 0);
        }

        CapturingSource(long frequency, ChannelCalculator calculator, List<float[]> captured, int forcedIndexCount)
        {
            super(new TunerChannel(frequency, 12500), calculator, new SynthesisFilterManager(), sourceEvent -> {},
                "test", null);
            mCaptured = captured;
            mForcedIndexes = new ArrayList<>();

            for(int x = 0; x < forcedIndexCount; x++)
            {
                mForcedIndexes.add(x);
            }
        }

        @Override
        public List<Integer> getOutputProcessorIndexes()
        {
            return mForcedIndexes.isEmpty() ? super.getOutputProcessorIndexes() : mForcedIndexes;
        }

        @Override
        public void receiveChannelResults(ChannelResultsBatch channelResults)
        {
            mCaptured.add(channelResults.getSamples().clone());
        }
    }
}
