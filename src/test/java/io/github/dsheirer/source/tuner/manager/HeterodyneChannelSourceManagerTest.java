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
package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.buffer.FloatNativeBuffer;
import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import io.github.dsheirer.source.tuner.test.TestTunerController;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the channel start/stop lifecycle of the heterodyne channel source manager.
 */
public class HeterodyneChannelSourceManagerTest
{
    private static final long FREQUENCY = 460_000_000L;
    private static final ChannelSpecification SPECIFICATION = new ChannelSpecification(25000.0, 12500, 6000.0, 7000.0);

    /**
     * Tuner controller that tracks sample buffer listeners, without generating any sample buffers.
     */
    private static class ListenerTrackingTunerController extends TestTunerController
    {
        private final Set<Listener<INativeBuffer>> mListeners = new HashSet<>();
        private boolean mTuningFails = false;

        /**
         * Sends a sample buffer to each of the listeners.
         */
        void deliver(long timestamp)
        {
            new ArrayList<>(mListeners).forEach(listener -> listener.receive(new FloatNativeBuffer(new float[4096],
                timestamp, 0.0f)));
        }

        @Override
        public void setFrequency(long frequency) throws SourceException
        {
            if(mTuningFails)
            {
                throw new SourceException("Tuner frequency cannot be set");
            }

            super.setFrequency(frequency);
        }

        @Override
        public void addBufferListener(Listener<INativeBuffer> listener)
        {
            mListeners.add(listener);
        }

        @Override
        public void removeBufferListener(Listener<INativeBuffer> listener)
        {
            mListeners.remove(listener);
        }

        @Override
        public boolean hasBufferListeners()
        {
            return !mListeners.isEmpty();
        }
    }

    private static TunerChannelSource source(HeterodyneChannelSourceManager manager, long frequency)
    {
        TunerChannelSource source = manager.getSource(new TunerChannel(frequency, 12500), SPECIFICATION, "test");
        assertNotNull(source);
        return source;
    }

    /**
     * Test: the tuner stops streaming when the last channel stops, before any further sample buffers arrive.
     */
    @Test
    void lastChannelStopDeregistersFromTuner()
    {
        ListenerTrackingTunerController controller = new ListenerTrackingTunerController();
        HeterodyneChannelSourceManager manager = new HeterodyneChannelSourceManager(controller);
        TunerChannelSource first = source(manager, FREQUENCY);
        TunerChannelSource second = source(manager, FREQUENCY + 25000);

        first.start();
        second.start();
        assertTrue(controller.hasBufferListeners());

        first.stop();
        assertTrue(controller.hasBufferListeners(), "tuner keeps streaming while a channel is running");

        second.stop();
        assertFalse(controller.hasBufferListeners(), "tuner stops streaming after the last channel stops");
        assertEquals(0, manager.getTunerChannelCount());
        assertFalse(controller.isLockedSampleRate());
    }

    /**
     * Test: streaming restarts for a channel that starts after the previous last channel stopped.
     */
    @Test
    void channelStartAfterLastChannelStopRegistersWithTuner()
    {
        ListenerTrackingTunerController controller = new ListenerTrackingTunerController();
        HeterodyneChannelSourceManager manager = new HeterodyneChannelSourceManager(controller);

        TunerChannelSource first = source(manager, FREQUENCY);
        first.start();
        first.stop();

        TunerChannelSource second = source(manager, FREQUENCY);
        second.start();
        assertTrue(controller.hasBufferListeners());
        second.stop();
        assertFalse(controller.hasBufferListeners());
    }

    /**
     * Test: no channel is provided when the tuner can't retune for it, and the running channel is unaffected.
     */
    @Test
    void failedRetuneDoesNotProvideChannel()
    {
        ListenerTrackingTunerController controller = new ListenerTrackingTunerController();
        HeterodyneChannelSourceManager manager = new HeterodyneChannelSourceManager(controller);
        TunerChannelSource running = source(manager, FREQUENCY);
        running.start();

        controller.mTuningFails = true;

        //Within the tuner bandwidth of the running channel, but outside of the current center frequency's bandwidth
        assertNull(manager.getSource(new TunerChannel(FREQUENCY + 2_000_000L, 12500), SPECIFICATION, "test"));
        assertEquals(1, manager.getTunerChannelCount());
        assertTrue(controller.isLockedSampleRate());
        running.stop();
    }

    /**
     * Test: a channel started with a grant timestamp is preloaded with the delayed samples from that timestamp forward,
     * and in order, ahead of the current samples.
     */
    @Test
    void channelIsPreloadedFromStartTimestamp() throws Exception
    {
        ListenerTrackingTunerController controller = new ListenerTrackingTunerController();
        HeterodyneChannelSourceManager manager = new HeterodyneChannelSourceManager(controller);
        TunerChannelSource running = source(manager, FREQUENCY);
        running.start();

        //The first buffer applies the running channel's pending start request
        for(long timestamp = 1000; timestamp <= 5000; timestamp += 1000)
        {
            controller.deliver(timestamp);
        }

        TunerChannelSource traffic = source(manager, FREQUENCY + 25000);
        CountDownLatch latch = new CountDownLatch(4);
        List<Long> timestamps = new CopyOnWriteArrayList<>();
        traffic.setListener((ComplexSamples samples) -> {
            timestamps.add(samples.timestamp());
            latch.countDown();
        });
        traffic.setStartTimestamp(3000);
        traffic.start();

        controller.deliver(6000);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(List.of(3000L, 4000L, 5000L, 6000L), timestamps);

        traffic.stop();
        running.stop();
    }

    /**
     * Test: channels that share a frequency are counted separately, and the tuner channel, the sample rate lock and the
     * tuner stream remain until the last of those channels stops.
     */
    @Test
    void channelsWithSameFrequencyAreTrackedSeparately()
    {
        ListenerTrackingTunerController controller = new ListenerTrackingTunerController();
        HeterodyneChannelSourceManager manager = new HeterodyneChannelSourceManager(controller);
        TunerChannelSource first = source(manager, FREQUENCY);
        TunerChannelSource second = source(manager, FREQUENCY);
        first.start();
        second.start();
        assertEquals(2, manager.getTunerChannelCount());

        first.stop();
        assertEquals(1, manager.getTunerChannelCount());
        assertEquals(1, manager.getTunerChannels().size());
        assertTrue(controller.isLockedSampleRate());
        assertTrue(controller.hasBufferListeners());

        second.stop();
        assertEquals(0, manager.getTunerChannelCount());
        assertTrue(manager.getTunerChannels().isEmpty());
        assertFalse(controller.isLockedSampleRate());
        assertFalse(controller.hasBufferListeners());
    }

    /**
     * Test: stopping a channel that was never started releases its tuner channel.
     */
    @Test
    void stopWithoutStartReleasesTunerChannel()
    {
        ListenerTrackingTunerController controller = new ListenerTrackingTunerController();
        HeterodyneChannelSourceManager manager = new HeterodyneChannelSourceManager(controller);

        source(manager, FREQUENCY).stop();

        assertEquals(0, manager.getTunerChannelCount());
        assertFalse(controller.isLockedSampleRate());
    }

    /**
     * Test: stopping a never started channel doesn't stop the sample buffers for a running channel.
     */
    @Test
    void stopWithoutStartLeavesRunningChannelStreaming()
    {
        ListenerTrackingTunerController controller = new ListenerTrackingTunerController();
        HeterodyneChannelSourceManager manager = new HeterodyneChannelSourceManager(controller);
        TunerChannelSource running = source(manager, FREQUENCY);
        TunerChannelSource neverStarted = source(manager, FREQUENCY + 25000);

        running.start();
        neverStarted.stop();

        assertTrue(controller.hasBufferListeners());
        running.stop();
    }
}
