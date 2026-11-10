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
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the shared ownership of channel results batch sample arrays.
 */
public class ChannelResultsBatchTest
{
    /**
     * Test: the sample array only returns to the pool after every reference is released.
     */
    @Test
    void samplesReturnToPoolAfterLastRelease()
    {
        Queue<float[]> pool = new ArrayDeque<>();
        float[] samples = new float[8];
        ChannelResultsBatch batch = new ChannelResultsBatch(samples, 4, 0, pool);
        batch.retain();
        batch.retain();

        batch.release();
        batch.release();
        assertTrue(pool.isEmpty(), "Returned to pool with an outstanding reference");

        batch.release();
        assertSame(samples, pool.poll());
    }

    /**
     * Test: blocks are derived from the array and block lengths.
     */
    @Test
    void blockCountIsDerivedFromLengths()
    {
        assertEquals(2, new ChannelResultsBatch(new float[8], 4, 0, new ArrayDeque<>()).getBlockCount());
    }

    /**
     * Test: an output processor that doesn't accept a batch because it isn't running doesn't keep a reference.
     */
    @Test
    void stoppedOutputProcessorReleasesBatch()
    {
        Queue<float[]> pool = new ArrayDeque<>();
        ChannelResultsBatch batch = new ChannelResultsBatch(new float[8], 4, 0, pool);
        OneChannelOutputProcessor processor = new OneChannelOutputProcessor(50000.0, List.of(0), 1.0f, null, "test");

        processor.receiveChannelResults(batch);
        batch.release();

        assertEquals(1, pool.size());
    }
}
