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

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import java.util.ArrayDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that a failure in one polyphase channel source is contained to that source and is reported to its consumer.
 */
public class PolyphaseChannelSourceTest
{
    private static final long CENTER = 100_000_000L;
    private static final double SAMPLE_RATE = 2_400_000.0;
    private static final int CHANNEL_COUNT = 96;

    private static ChannelCalculator calculator(long center)
    {
        return new ChannelCalculator(SAMPLE_RATE, CHANNEL_COUNT, center, 2.0);
    }

    private static ChannelResultsBatch batch()
    {
        return new ChannelResultsBatch(new float[CHANNEL_COUNT * 2], CHANNEL_COUNT * 2, 0, new ArrayDeque<>());
    }

    private static PolyphaseChannelSource source()
    {
        return new PolyphaseChannelSource(new TunerChannel(CENTER + 125000, 12500), calculator(CENTER),
            new SynthesisFilterManager(), sourceEvent -> {}, "test", null);
    }

    /**
     * Test: an output processor update failure doesn't throw to the channelizer, signals one error to the consumer and
     * stops this source from processing further results.
     */
    @Test
    void failedOutputProcessorUpdateSignalsErrorOnce() throws Exception
    {
        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicInteger errorCount = new AtomicInteger();
        Listener<SourceEvent> consumer = sourceEvent -> {
            if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_ERROR_STATE)
            {
                errorCount.incrementAndGet();
                errorLatch.countDown();
            }
        };

        PolyphaseChannelSource source = source();
        source.setSourceEventListener(consumer);

        //Retune so that the channel is outside of the tuner bandwidth
        source.updateOutputProcessor(calculator(CENTER + 10_000_000L), new SynthesisFilterManager());

        source.applyPendingOutputProcessorUpdate();
        source.receiveChannelResults(batch());
        source.applyPendingOutputProcessorUpdate();
        source.receiveChannelResults(batch());

        assertTrue(errorLatch.await(5, TimeUnit.SECONDS), "Error was not signalled to the consumer");
        Thread.sleep(50);
        assertEquals(1, errorCount.get());
    }

    /**
     * Test: an exception from one channel doesn't stop delivery of the results to the following channels.
     */
    @Test
    void channelFailureDoesNotAffectFollowingChannels()
    {
        AtomicInteger received = new AtomicInteger();

        PolyphaseChannelSource failing = new TestSource()
        {
            @Override
            public void receiveChannelResults(ChannelResultsBatch channelResults)
            {
                throw new IllegalStateException("Test failure");
            }
        };

        PolyphaseChannelSource following = new TestSource()
        {
            @Override
            public void receiveChannelResults(ChannelResultsBatch channelResults)
            {
                received.incrementAndGet();
            }
        };

        ComplexPolyphaseChannelizerM2 channelizer = new ComplexPolyphaseChannelizerM2(new float[CHANNEL_COUNT * 9],
            (int)SAMPLE_RATE, CHANNEL_COUNT);
        channelizer.addChannel(failing);
        channelizer.addChannel(following);

        channelizer.prepareChannels();
        channelizer.dispatch(batch());

        assertEquals(1, received.get());
    }

    /**
     * Polyphase channel source for overriding the channel results processing.
     */
    private static class TestSource extends PolyphaseChannelSource
    {
        TestSource()
        {
            super(new TunerChannel(CENTER + 125000, 12500), calculator(CENTER), new SynthesisFilterManager(),
                sourceEvent -> {}, "test", null);
        }
    }
}
