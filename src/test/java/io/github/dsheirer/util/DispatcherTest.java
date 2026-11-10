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
package io.github.dsheirer.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies dispatcher queue limits.
 */
public class DispatcherTest
{
    private static final int MAXIMUM_QUEUE_SIZE = 4;

    /**
     * Sends the count of elements while the dispatcher's listener is blocked on the first element.
     * @return elements received by the listener after it is unblocked
     */
    private static List<Integer> dispatchWhileBlocked(Dispatcher<Integer> dispatcher, int count) throws Exception
    {
        List<Integer> received = new CopyOnWriteArrayList<>();
        CountDownLatch blocked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        dispatcher.setListener(element -> {
            received.add(element);
            blocked.countDown();

            try
            {
                release.await();
            }
            catch(InterruptedException ie)
            {
                Thread.currentThread().interrupt();
            }
        });

        dispatcher.start();
        dispatcher.receive(0);
        assertTrue(blocked.await(5, TimeUnit.SECONDS), "Listener did not receive the first element");

        for(int x = 1; x <= count; x++)
        {
            dispatcher.receive(x);
        }

        release.countDown();
        return received;
    }

    private static void awaitSize(List<Integer> list, int size) throws Exception
    {
        long timeout = System.currentTimeMillis() + 5000;

        while(list.size() < size && System.currentTimeMillis() < timeout)
        {
            Thread.sleep(5);
        }
    }

    /**
     * Test: elements received while a bounded queue is full are dropped and the queued elements stay in order.
     */
    @Test
    void boundedQueueDropsNewestElementsWhenFull() throws Exception
    {
        Dispatcher<Integer> dispatcher = new Dispatcher<>("test", 1, MAXIMUM_QUEUE_SIZE);
        List<Integer> received = dispatchWhileBlocked(dispatcher, MAXIMUM_QUEUE_SIZE + 10);
        awaitSize(received, MAXIMUM_QUEUE_SIZE + 1);
        Thread.sleep(50);
        dispatcher.stop();

        assertEquals(List.of(0, 1, 2, 3, 4), received);
    }

    /**
     * Test: a bounded dispatcher resumes delivery after an overflow.
     */
    @Test
    void boundedQueueRecoversAfterOverflow() throws Exception
    {
        Dispatcher<Integer> dispatcher = new Dispatcher<>("test", 1, MAXIMUM_QUEUE_SIZE);
        List<Integer> received = dispatchWhileBlocked(dispatcher, MAXIMUM_QUEUE_SIZE + 10);
        awaitSize(received, MAXIMUM_QUEUE_SIZE + 1);
        dispatcher.receive(99);
        awaitSize(received, MAXIMUM_QUEUE_SIZE + 2);
        dispatcher.stop();

        assertEquals(99, received.get(received.size() - 1));
    }

    /**
     * Test: an unbounded dispatcher doesn't drop elements.
     */
    @Test
    void unboundedQueueKeepsAllElements() throws Exception
    {
        Dispatcher<Integer> dispatcher = new Dispatcher<>("test", 1);
        List<Integer> received = dispatchWhileBlocked(dispatcher, 100);
        awaitSize(received, 101);
        dispatcher.stop();

        assertEquals(101, received.size());
    }
}
