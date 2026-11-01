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

package io.github.dsheirer.sample;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SerializedListenerTest
{
    /**
     * Test: a second thread waits until the first thread leaves the listener.
     */
    @Test
    void secondThreadWaitsForFirstThreadToLeaveListener() throws Exception
    {
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);

        SerializedListener<Integer> listener = new SerializedListener<>();
        listener.setListener(value -> {
            if(value == 1)
            {
                firstEntered.countDown();
                await(releaseFirst);
            }
            else
            {
                secondEntered.countDown();
            }
        });

        Thread first = new Thread(() -> listener.receive(1));
        first.start();
        assertTrue(firstEntered.await(5, TimeUnit.SECONDS));

        Thread second = new Thread(() -> listener.receive(2));
        second.start();
        assertFalse(secondEntered.await(200, TimeUnit.MILLISECONDS), "Threads overlapped in the listener");

        releaseFirst.countDown();
        assertTrue(secondEntered.await(5, TimeUnit.SECONDS), "Second thread was not released");
        first.join(5000);
        second.join(5000);
    }

    /**
     * Test: a listener exception releases the lock for the next delivery.
     */
    @Test
    void listenerExceptionReleasesLock() throws Exception
    {
        AtomicInteger received = new AtomicInteger();
        SerializedListener<Integer> listener = new SerializedListener<>();
        listener.setListener(value -> {
            if(value == 1)
            {
                throw new IllegalStateException("Test failure");
            }
            received.incrementAndGet();
        });

        assertThrows(IllegalStateException.class, () -> listener.receive(1));

        Thread other = new Thread(() -> listener.receive(2));
        other.start();
        other.join(5000);

        assertEquals(1, received.get());
    }

    /**
     * Test: elements are ignored when there isn't a listener.
     */
    @Test
    void nullListenerIgnoresElements()
    {
        assertDoesNotThrow(() -> new SerializedListener<Integer>().receive(1));
    }

    private static void await(CountDownLatch latch)
    {
        try
        {
            latch.await(10, TimeUnit.SECONDS);
        }
        catch(InterruptedException ie)
        {
            Thread.currentThread().interrupt();
        }
    }
}
