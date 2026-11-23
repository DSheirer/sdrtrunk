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

import io.github.dsheirer.sample.Listener;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests timestamp-based replay, listener removal, buffer clearing, and delivery of live samples.
 */
class NativeSampleDelayBufferTest
{
    @Test
    void replaysRetainedBuffersInOrderFromRequestedTime()
    {
        NativeSampleDelayBuffer delay = new NativeSampleDelayBuffer(4, 10);
        delay.receive(samplesAt(100));
        delay.receive(samplesAt(110));
        delay.receive(samplesAt(120));
        delay.receive(samplesAt(130));
        delay.receive(samplesAt(140));

        List<Long> timestamps = new ArrayList<>();
        delay.addListener(buffer -> timestamps.add(buffer.getTimestamp()), 130);
        assertFalse(delay.hasListeners());
        delay.receive(samplesAt(150));

        assertTrue(delay.hasListeners());
        assertEquals(List.of(120L, 130L, 140L, 150L), timestamps);
    }

    @Test
    void appliesClearAndRemovalBeforeDeliveringNextBuffer()
    {
        NativeSampleDelayBuffer delay = new NativeSampleDelayBuffer(2, 10);
        INativeBuffer past = samplesAt(100);
        INativeBuffer current = samplesAt(110);
        List<INativeBuffer> received = new ArrayList<>();
        Listener<INativeBuffer> listener = received::add;

        delay.receive(past);
        delay.clear();
        delay.addListener(listener, 100);
        delay.receive(current);
        assertEquals(List.of(current), received);

        delay.removeListener(listener);
        delay.receive(samplesAt(120));
        assertFalse(delay.hasListeners());
        assertEquals(List.of(current), received);
    }

    @Test
    void sharesLiveBufferAcrossMultipleListeners()
    {
        NativeSampleDelayBuffer delay = new NativeSampleDelayBuffer(2, 10);
        List<INativeBuffer> first = new ArrayList<>();
        List<INativeBuffer> second = new ArrayList<>();
        delay.addListener(first::add, 100);
        delay.addListener(second::add, 100);
        delay.receive(samplesAt(100));

        assertEquals(1, first.size());
        assertEquals(1, second.size());
        assertSame(first.getFirst(), second.getFirst());
        assertInstanceOf(SharedNativeBuffer.class, first.getFirst());
        assertEquals(100, first.getFirst().getTimestamp());

        delay.dispose();
        assertFalse(delay.hasListeners());
    }

    private static INativeBuffer samplesAt(long timestamp)
    {
        return new FloatNativeBuffer(new float[]{1, -1}, timestamp, 1);
    }
}
