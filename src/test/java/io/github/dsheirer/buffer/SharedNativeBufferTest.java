/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies that channels sharing a native buffer receive the same converted samples.
 */
public class SharedNativeBufferTest
{
    private static byte[] samples()
    {
        byte[] samples = new byte[8192 * 8];
        new Random(1).nextBytes(samples);
        return samples;
    }

    private static List<ComplexSamples> read(INativeBuffer buffer)
    {
        List<ComplexSamples> fragments = new ArrayList<>();
        Iterator<ComplexSamples> iterator = buffer.iterator();

        while(iterator.hasNext())
        {
            fragments.add(iterator.next());
        }

        return fragments;
    }

    /**
     * Test: the samples match the wrapped buffer and each consumer gets the same instances.
     */
    @Test
    void consumersShareOneConversion()
    {
        ByteNativeBuffer nativeBuffer = new ByteNativeBuffer(samples(), 1000L, 0.0f, 2400.0f);
        SharedNativeBuffer shared = new SharedNativeBuffer(nativeBuffer);
        List<ComplexSamples> expected = read(nativeBuffer);
        List<ComplexSamples> first = read(shared);
        List<ComplexSamples> second = read(shared);

        assertEquals(expected.size(), first.size());
        assertEquals(expected.size(), second.size());

        for(int x = 0; x < expected.size(); x++)
        {
            assertArrayEquals(expected.get(x).i(), first.get(x).i());
            assertArrayEquals(expected.get(x).q(), first.get(x).q());
            assertEquals(expected.get(x).timestamp(), first.get(x).timestamp());
            assertSame(first.get(x), second.get(x));
        }
    }

    /**
     * Test: concurrent consumers each receive every fragment in order.
     */
    @Test
    void concurrentConsumersReceiveAllFragments() throws Exception
    {
        ByteNativeBuffer nativeBuffer = new ByteNativeBuffer(samples(), 1000L, 0.0f, 2400.0f);
        int fragmentCount = read(nativeBuffer).size();
        ExecutorService executor = Executors.newFixedThreadPool(4);

        try
        {
            for(int trial = 0; trial < 200; trial++)
            {
                SharedNativeBuffer shared = new SharedNativeBuffer(nativeBuffer);
                List<Future<List<ComplexSamples>>> results = new ArrayList<>();

                for(int consumer = 0; consumer < 4; consumer++)
                {
                    results.add(executor.submit(() -> read(shared)));
                }

                List<ComplexSamples> first = results.getFirst().get();
                assertEquals(fragmentCount, first.size());

                for(Future<List<ComplexSamples>> result : results)
                {
                    assertEquals(first, result.get());
                }
            }
        }
        finally
        {
            executor.shutdownNow();
        }
    }
}
