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

import io.github.dsheirer.dsp.filter.channelizer.output.OneChannelOutputProcessor;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that channel results batches keep their own timestamp and configuration through the threaded dispatchers.
 */
public class ChannelResultsTimestampTest
{
    private static final int BATCH_SIZE = 2048;
    private static final int CHANNEL_COUNT = 8;
    private static final int SAMPLES_PER_BATCH = 1024 * CHANNEL_COUNT / 2;

    private static void awaitSize(List<?> list, int size) throws Exception
    {
        long timeout = System.currentTimeMillis() + 5000;

        while(list.size() < size && System.currentTimeMillis() < timeout)
        {
            Thread.sleep(5);
        }
    }

    private static ChannelResultsBatch batch(float i, float q, long timestamp)
    {
        float[] samples = new float[BATCH_SIZE * 4];

        for(int x = 0; x < samples.length; x += 4)
        {
            samples[x] = i;
            samples[x + 1] = q;
            samples[x + 2] = -i;
            samples[x + 3] = -q;
        }

        return new ChannelResultsBatch(samples, 4, timestamp, new ArrayDeque<>());
    }

    /**
     * Test: each batch dispatched by the channelizer has the timestamp of the sample buffer that completed the batch,
     * even when a later sample buffer arrives before the batch is dispatched.
     */
    @Test
    void channelizerDispatchesEachBatchWithItsOwnTimestamp() throws Exception
    {
        List<Long> timestamps = new CopyOnWriteArrayList<>();
        ComplexPolyphaseChannelizerM2 channelizer = new ComplexPolyphaseChannelizerM2(new float[CHANNEL_COUNT * 9],
            25000 * CHANNEL_COUNT, CHANNEL_COUNT)
        {
            @Override
            protected void dispatch(ChannelResultsBatch channelResults)
            {
                timestamps.add(channelResults.getTimestamp());
            }
        };

        channelizer.start();

        for(long timestamp : new long[]{10, 20, 30})
        {
            channelizer.receive(new InterleavedComplexSamples(new float[SAMPLES_PER_BATCH * 2], timestamp));
        }

        awaitSize(timestamps, 3);
        channelizer.stop();

        assertEquals(List.of(10L, 20L, 30L), timestamps);
    }

    /**
     * Test: assembled sample buffers have the timestamp of their batch, and a configuration change requested between
     * two batches takes effect at the start of the second batch, even when the first batch hasn't been processed yet.
     */
    @Test
    void outputProcessorAppliesTimestampAndConfigurationPerBatch() throws Exception
    {
        List<ComplexSamples> output = new CopyOnWriteArrayList<>();
        OneChannelOutputProcessor processor = new OneChannelOutputProcessor(50000.0, List.of(0), 1.0f, null, "test");
        processor.setListener(output::add);

        processor.start();
        processor.receiveChannelResults(batch(1.0f, 2.0f, 100));
        processor.setPolyphaseChannelIndices(List.of(1));
        processor.receiveChannelResults(batch(1.0f, 2.0f, 200));
        awaitSize(output, 2);
        processor.stop();

        float[] expected = new float[BATCH_SIZE];
        assertEquals(100, output.get(0).timestamp());
        Arrays.fill(expected, 1.0f);
        assertArrayEquals(expected, output.get(0).i());
        assertEquals(200, output.get(1).timestamp());
        Arrays.fill(expected, -1.0f);
        assertArrayEquals(expected, output.get(1).i());
    }
}
