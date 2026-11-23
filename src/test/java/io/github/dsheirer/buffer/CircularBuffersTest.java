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

import io.github.dsheirer.sample.complex.Complex;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests eviction order, sample access across wraparound, and statistics for circular buffers.
 */
class CircularBuffersTest
{
    @Test
    void broadcastsEvictedElementsInArrivalOrder()
    {
        CircularBuffer<String> buffer = new CircularBuffer<>(2);
        List<String> evicted = new ArrayList<>();
        buffer.setListener(evicted::add);

        assertEquals(2, buffer.getSize());
        assertNull(buffer.get(0));
        buffer.receive("first");
        buffer.receive("second");
        assertTrue(evicted.isEmpty());

        buffer.receive("third");
        assertEquals("third", buffer.get(0));
        assertEquals(List.of("third", "second"), buffer.getElements());
        assertThrows(UnsupportedOperationException.class, () -> buffer.getElements().clear());

        buffer.receive("fourth");
        assertEquals(List.of("first", "second"), evicted);
    }

    @Test
    void floatBufferOrdersSamplesAndComputesStatisticsAfterWrapping()
    {
        FloatCircularBuffer buffer = new FloatCircularBuffer(3, -1);
        assertEquals(-1, buffer.get(2));
        buffer.put(-4);
        buffer.put(6);
        assertEquals(2, buffer.get(8));

        assertArrayEquals(new float[]{-4, 6, 8}, buffer.getAll());
        assertArrayEquals(new float[]{8, 6, -4}, buffer.getAllReversed());
        assertEquals(-4, buffer.min());
        assertEquals(10.0f / 3, buffer.mean());
        assertEquals(8, buffer.max(0));
        assertEquals(10, buffer.max(10));

        buffer.reset(3);
        assertArrayEquals(new float[]{3, 3, 3}, buffer.getAll());
    }

    @Test
    void doubleBufferComputesSampleStatisticsAfterWrapping()
    {
        DoubleCircularBuffer buffer = new DoubleCircularBuffer(3, -1);
        assertEquals(-1, buffer.get(2));
        buffer.put(4);
        buffer.put(6);
        assertEquals(2, buffer.get(8));

        assertArrayEquals(new double[]{4, 6, 8}, buffer.getAll());
        assertEquals(4, buffer.min());
        assertEquals(8, buffer.max());
        assertEquals(6, buffer.mean());
        assertEquals(4, buffer.variance());
        assertEquals(2, buffer.standardDeviation());
    }

    @Test
    void complexBufferReturnsOldestSamplesInOrderAcrossWrap()
    {
        ComplexCircularBuffer buffer = new ComplexCircularBuffer(2);
        Complex first = new Complex(2, -2);
        Complex second = new Complex(3, -3);
        Complex third = new Complex(4, -4);
        assertEquals(2, buffer.length());

        buffer.put(first);
        buffer.put(second);
        assertSame(first, buffer.get(third));
        assertArrayEquals(new Complex[]{second}, buffer.get(1));
        assertArrayEquals(new Complex[]{second, third}, buffer.getAll());
    }

    @Test
    void tappedComplexBufferIndexesFromNewestToOldest()
    {
        ComplexTappedCircularBuffer buffer = new ComplexTappedCircularBuffer(2);
        assertEquals(0, buffer.get(1).inphase());
        assertEquals(0, buffer.get(1).quadrature());

        Complex first = new Complex(1, -1);
        Complex second = new Complex(2, -2);
        Complex third = new Complex(3, -3);
        buffer.add(first);
        assertSame(first, buffer.get(0));
        buffer.add(second);
        assertSame(first, buffer.get(1));
        buffer.add(third);
        assertSame(third, buffer.get(0));
        assertSame(second, buffer.get(1));
    }
}
