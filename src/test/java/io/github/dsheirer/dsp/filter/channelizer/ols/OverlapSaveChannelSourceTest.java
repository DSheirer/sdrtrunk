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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.dsheirer.buffer.FloatNativeBuffer;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DecoderLSM;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Overlap-save channel source block pool: the pool never hands out a block in use, the threaded source delivers the
 * same samples as an inline channel while its blocks are reused, and the channelizer thread doesn't allocate in steady
 * state.
 */
public class OverlapSaveChannelSourceTest
{
    private static final double SAMPLE_RATE = 2400000.0;
    private static final long TUNER_FREQUENCY = 450000000;
    private static final ChannelSpecification SPECIFICATION = new ChannelSpecification(24000.0, 12500, 5750.0, 6500.0,
        P25P1DecoderLSM.getChannelFilter());

    @Test
    void blockPoolOrderAndCapacity()
    {
        OverlapSaveChannelSource.BlockPool pool = new OverlapSaveChannelSource.BlockPool(3);
        assertNull(pool.acquire());
        OverlapSaveChannelSource.QueuedBlock[] blocks = new OverlapSaveChannelSource.QueuedBlock[4];

        for(int x = 0; x < blocks.length; x++)
        {
            blocks[x] = new OverlapSaveChannelSource.QueuedBlock(2);
        }

        assertTrue(pool.release(blocks[0]));
        assertTrue(pool.release(blocks[1]));
        assertTrue(pool.release(blocks[2]));
        assertFalse(pool.release(blocks[3]), "full pool");
        assertEquals(3, pool.size());
        assertSame(blocks[0], pool.acquire());
        assertTrue(pool.release(blocks[3]));
        assertSame(blocks[1], pool.acquire());
        assertSame(blocks[2], pool.acquire());
        assertSame(blocks[3], pool.acquire());
        assertNull(pool.acquire());
        assertEquals(0, pool.size());
    }

    /**
     * A producer thread acquires blocks (or creates one when the pool is empty), stamps them and hands them to a
     * consumer thread, which checks the stamp and releases them: no block is acquired while it's in flight, no stamp is
     * overwritten in flight, and the pool recycles the blocks.
     */
    @Test
    void blockPoolNeverHandsOutBlockInUse() throws Exception
    {
        int handoffCapacity = 50;
        int count = 500000;
        OverlapSaveChannelSource.BlockPool pool = new OverlapSaveChannelSource.BlockPool(handoffCapacity + 2);
        BlockingQueue<OverlapSaveChannelSource.QueuedBlock> handoff = new ArrayBlockingQueue<>(handoffCapacity);
        Set<OverlapSaveChannelSource.QueuedBlock> inFlight = Collections.newSetFromMap(new ConcurrentHashMap<>());
        AtomicReference<String> failure = new AtomicReference<>();
        AtomicInteger created = new AtomicInteger();

        Thread producer = new Thread(() -> {
            try
            {
                for(int x = 0; x < count && failure.get() == null; x++)
                {
                    OverlapSaveChannelSource.QueuedBlock block = pool.acquire();

                    if(block == null)
                    {
                        block = new OverlapSaveChannelSource.QueuedBlock(2);
                        created.incrementAndGet();
                    }

                    if(!inFlight.add(block))
                    {
                        failure.compareAndSet(null, "acquired a block in flight at " + x);
                    }

                    block.mBlock = x;
                    block.mBins[0] = x;
                    handoff.put(block);
                }
            }
            catch(InterruptedException ie)
            {
                failure.compareAndSet(null, "producer interrupted");
            }
        });

        Thread consumer = new Thread(() -> {
            try
            {
                for(int x = 0; x < count && failure.get() == null; x++)
                {
                    OverlapSaveChannelSource.QueuedBlock block = handoff.poll(10, TimeUnit.SECONDS);

                    if(block == null)
                    {
                        failure.compareAndSet(null, "consumer timed out at " + x);
                        return;
                    }

                    //Spin now and then, so that the producer runs ahead and drains the pool
                    if(x % 97 == 0)
                    {
                        long end = System.nanoTime() + 20000;

                        while(System.nanoTime() < end)
                        {
                            Thread.onSpinWait();
                        }
                    }

                    if(block.mBlock != x || block.mBins[0] != (float)x)
                    {
                        failure.compareAndSet(null, "block " + x + " overwritten in flight with " + block.mBlock);
                    }

                    if(!inFlight.remove(block))
                    {
                        failure.compareAndSet(null, "released a block that wasn't in flight at " + x);
                    }

                    pool.release(block);
                }
            }
            catch(InterruptedException ie)
            {
                failure.compareAndSet(null, "consumer interrupted");
            }
        });

        producer.start();
        consumer.start();
        producer.join(60000);
        consumer.join(60000);
        assertFalse(producer.isAlive() || consumer.isAlive(), "threads finished");
        assertNull(failure.get());
        //At most the handoff queue, one block held by each thread
        assertTrue(created.get() <= handoffCapacity + 2, "created " + created.get() + " blocks");
    }

    /**
     * A threaded source and an inline channel with the same design receive the same blocks.  The source's consumer is
     * slow now and then while the channelizer keeps queueing, so blocks are reused while others are in flight.  Every
     * source output must match the inline output bit for bit, which fails if a block's bins are overwritten while the
     * dispatcher thread is processing them.
     */
    @Test
    void sourceMatchesInlineChannelWhileBlocksAreReused() throws Exception
    {
        OverlapSaveChannelizer channelizer = OverlapSaveChannelizer.create(SAMPLE_RATE);
        double offset = -123456.0;
        Map<Long, ComplexSamples> inline = new ConcurrentHashMap<>();
        Map<Long, ComplexSamples> threaded = new ConcurrentHashMap<>();
        AtomicInteger received = new AtomicInteger();

        OverlapSaveChannel inlineChannel = OverlapSaveChannelSource.createChannel(channelizer, offset, SPECIFICATION);
        inlineChannel.setListener(samples -> inline.put(samples.timestamp(), samples));
        channelizer.addChannel(inlineChannel);

        OverlapSaveChannelSource source = source(channelizer, offset, samples -> {
            threaded.put(samples.timestamp(), samples);

            if(received.incrementAndGet() % 5 == 0)
            {
                sleep(1);
            }
        });

        float[][] noise = noise(7, channelizer.getHop(), new Random(11));
        int bursts = 10;
        int burst = 30;
        long timestamp = 1700000000000L;
        int produced = 0;
        source.start();

        try
        {
            for(int b = 0; b < bursts; b++)
            {
                for(int x = 0; x < burst; x++)
                {
                    channelizer.receive(noise[produced % noise.length], timestamp + 20L * produced);
                    produced++;
                    sleep(1);
                }

                awaitCount(received, produced);
            }
        }
        finally
        {
            source.stop();
        }

        assertEquals(produced, inline.size(), "inline blocks");
        assertEquals(produced, threaded.size(), "threaded blocks");

        for(Map.Entry<Long, ComplexSamples> entry : inline.entrySet())
        {
            ComplexSamples actual = threaded.get(entry.getKey());
            assertNotNull(actual, "block at " + entry.getKey());
            assertArrayEquals(entry.getValue().i(), actual.i(), "inphase at " + entry.getKey());
            assertArrayEquals(entry.getValue().q(), actual.q(), "quadrature at " + entry.getKey());
        }
    }

    /**
     * The channelizer thread, from a float native buffer through the forward FFT to queueing each channel's block,
     * doesn't allocate once the block pools hold enough blocks.
     */
    @Test
    void channelizerThreadDoesNotAllocate() throws Exception
    {
        assumeTrue(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        assumeTrue(bean.isThreadAllocatedMemorySupported() && bean.isThreadAllocatedMemoryEnabled());

        OverlapSaveChannelizer channelizer = OverlapSaveChannelizer.create(SAMPLE_RATE);
        int channels = 4;
        AtomicInteger received = new AtomicInteger();
        AtomicReference<CountDownLatch> gate = new AtomicReference<>();
        List<OverlapSaveChannelSource> sources = new ArrayList<>();

        for(int c = 0; c < channels; c++)
        {
            sources.add(source(channelizer, -300000.0 + c * 150000.0, samples -> {
                CountDownLatch latch = gate.get();

                if(latch != null)
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

                received.incrementAndGet();
            }));
        }

        //One hop per buffer, prepared up front with timestamps 20 ms apart
        int gatedBurst = 25;
        int warmUpBursts = 10;
        int measuredBursts = 5;
        int burst = 20;
        int total = gatedBurst + (warmUpBursts + measuredBursts) * burst;
        float[][] noise = noise(5, channelizer.getHop(), new Random(5));
        FloatNativeBuffer[] buffers = new FloatNativeBuffer[total];

        for(int x = 0; x < total; x++)
        {
            buffers[x] = new FloatNativeBuffer(noise[x % noise.length], 1700000000000L + 20L * x, 2400.0f);
        }

        for(OverlapSaveChannelSource source : sources)
        {
            source.start();
        }

        try
        {
            int pointer = 0;

            //With the consumers held, every block of this burst is in flight at once, so each pool ends up holding
            //more blocks than a later burst can have in flight
            CountDownLatch latch = new CountDownLatch(1);
            gate.set(latch);

            for(int x = 0; x < gatedBurst; x++)
            {
                OverlapSaveChannelManager.receive(channelizer, buffers[pointer++]);
            }

            gate.set(null);
            latch.countDown();
            awaitCount(received, pointer * channels);

            for(int b = 0; b < warmUpBursts; b++)
            {
                for(int x = 0; x < burst; x++)
                {
                    OverlapSaveChannelManager.receive(channelizer, buffers[pointer++]);
                }

                awaitCount(received, pointer * channels);
            }

            long thread = Thread.currentThread().threadId();
            long allocated = 0;

            for(int b = 0; b < measuredBursts; b++)
            {
                long before = bean.getThreadAllocatedBytes(thread);

                for(int x = 0; x < burst; x++)
                {
                    OverlapSaveChannelManager.receive(channelizer, buffers[pointer++]);
                }

                allocated += bean.getThreadAllocatedBytes(thread) - before;
                awaitCount(received, pointer * channels);
            }

            //A few bytes are tolerated for a lock node when queueing a block meets the dispatcher draining the queue
            assertTrue(allocated < 1024, "allocated " + allocated + " bytes over " + measuredBursts * burst +
                " blocks");
        }
        finally
        {
            for(OverlapSaveChannelSource source : sources)
            {
                source.stop();
            }
        }
    }

    /**
     * Creates a source whose sample stream requests add it to and remove it from the channelizer.
     */
    private static OverlapSaveChannelSource source(OverlapSaveChannelizer channelizer, double offset,
                                                   Listener<ComplexSamples> listener)
    {
        TunerChannel tunerChannel = new TunerChannel(TUNER_FREQUENCY + (long)offset, 12500);
        OverlapSaveChannelSource[] holder = new OverlapSaveChannelSource[1];
        Listener<SourceEvent> producer = event -> {
            if(event.getEvent() == SourceEvent.Event.REQUEST_START_SAMPLE_STREAM)
            {
                channelizer.addChannel(holder[0]);
            }
            else if(event.getEvent() == SourceEvent.Event.REQUEST_STOP_SAMPLE_STREAM)
            {
                channelizer.removeChannel(holder[0]);
            }
        };
        OverlapSaveChannelSource source = new OverlapSaveChannelSource(producer, tunerChannel, SPECIFICATION,
            channelizer, TUNER_FREQUENCY, "test overlap-save channel " + offset, null);
        holder[0] = source;
        source.setListener(listener);
        return source;
    }

    private static float[][] noise(int buffers, int samples, Random random)
    {
        float[][] noise = new float[buffers][samples * 2];

        for(float[] buffer : noise)
        {
            for(int x = 0; x < buffer.length; x++)
            {
                buffer[x] = random.nextFloat() * 2.0f - 1.0f;
            }
        }

        return noise;
    }

    /**
     * Waits for the count, then a little longer so that the dispatcher threads have returned their last blocks.
     */
    private static void awaitCount(AtomicInteger count, int expected) throws InterruptedException
    {
        long end = System.currentTimeMillis() + 10000;

        while(count.get() < expected && System.currentTimeMillis() < end)
        {
            Thread.sleep(5);
        }

        assertEquals(expected, count.get(), "blocks delivered");
        Thread.sleep(20);
    }

    private static void sleep(long milliseconds)
    {
        try
        {
            Thread.sleep(milliseconds);
        }
        catch(InterruptedException ie)
        {
            Thread.currentThread().interrupt();
        }
    }
}
