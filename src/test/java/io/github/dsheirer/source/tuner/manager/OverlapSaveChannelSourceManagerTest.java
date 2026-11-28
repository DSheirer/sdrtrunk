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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.buffer.FloatNativeBuffer;
import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.dsp.filter.channelizer.PolyphaseChannelSource;
import io.github.dsheirer.dsp.filter.channelizer.ols.OverlapSaveChannelSource;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DecoderLSM;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import io.github.dsheirer.source.tuner.test.TestTunerController;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Overlap-save channel source manager: channel lifecycle, center frequency selection and retuning at a block boundary,
 * and the polyphase fallback for unsupported sample rates.
 */
public class OverlapSaveChannelSourceManagerTest
{
    private static final ChannelSpecification SPECIFICATION = new ChannelSpecification(24000.0, 12500, 5750.0, 6500.0,
        P25P1DecoderLSM.getChannelFilter());

    /**
     * Tuner controller that tracks sample buffer listeners and delivers empty buffers on request.
     */
    private static class ListenerTrackingTunerController extends TestTunerController
    {
        private final Set<Listener<INativeBuffer>> mListeners = new CopyOnWriteArraySet<>();

        void deliver(int samples)
        {
            for(int sent = 0; sent < samples; sent += 8192)
            {
                new ArrayList<>(mListeners).forEach(listener -> listener.receive(new FloatNativeBuffer(new float[16384],
                    System.currentTimeMillis(), 0.0f)));
            }
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

    private static TunerChannelSource source(ChannelSourceManager manager, long frequency)
    {
        TunerChannelSource source = manager.getSource(new TunerChannel(frequency, 12500), SPECIFICATION, "test");
        assertNotNull(source);
        return source;
    }

    @Test
    void lifecycle()
    {
        ListenerTrackingTunerController controller = new ListenerTrackingTunerController();
        OverlapSaveChannelSourceManager manager = new OverlapSaveChannelSourceManager(controller);
        TunerChannelSource first = source(manager, controller.getFrequency() + 100000);
        TunerChannelSource second = source(manager, controller.getFrequency() - 337500);
        assertInstanceOf(OverlapSaveChannelSource.class, first);
        assertTrue(((OverlapSaveChannelSource)first).isChannelFiltered());
        assertEquals(25000.0, first.getSampleRate(), 0.001);

        first.start();
        second.start();
        assertTrue(controller.hasBufferListeners());
        assertTrue(controller.isLockedSampleRate());
        assertEquals(2, manager.getTunerChannelCount());

        first.stop();
        assertTrue(controller.hasBufferListeners(), "tuner keeps streaming while a channel is running");
        second.stop();
        assertFalse(controller.hasBufferListeners(), "tuner stops streaming after the last channel stops");
        assertEquals(0, manager.getTunerChannelCount());
        assertFalse(controller.isLockedSampleRate());
    }

    /**
     * A channel outside the current bandwidth retunes the tuner to the middle of the channel set, and the running
     * channel follows the new center frequency from the next block boundary.
     */
    @Test
    void retuneAtBlockBoundary() throws Exception
    {
        ListenerTrackingTunerController controller = new ListenerTrackingTunerController();
        OverlapSaveChannelSourceManager manager = new OverlapSaveChannelSourceManager(controller);
        long initial = controller.getFrequency();
        OverlapSaveChannelSource running = (OverlapSaveChannelSource)source(manager, initial + 900000);
        running.start();
        assertEquals(initial, running.getTunerFrequency());

        //1.9 MHz above the running channel: the pair spans 1.9125 MHz of the 2.4 MHz bandwidth
        OverlapSaveChannelSource added = (OverlapSaveChannelSource)source(manager, initial + 2800000);
        long expected = (initial + 900000 - 6250 + initial + 2800000 + 6250) / 2;
        assertEquals(expected, controller.getFrequency(), "tuner centered on the channel set");
        assertEquals(expected, added.getTunerFrequency());
        assertEquals(initial, running.getTunerFrequency(), "running channel waits for a block boundary");

        controller.deliver(70000);
        long deadline = System.currentTimeMillis() + 5000;

        while(running.getTunerFrequency() != expected && System.currentTimeMillis() < deadline)
        {
            Thread.sleep(10);
        }

        assertEquals(expected, running.getTunerFrequency(), "running channel follows the retune");
        running.stop();
    }

    /**
     * With 100% usable bandwidth, channels at the band edges are accepted, and a retune that leaves a running channel
     * at the edge keeps it running: the manager and the channel source agree on what fits.
     */
    @Test
    void bandEdgeRetuneKeepsRunningChannel() throws Exception
    {
        ListenerTrackingTunerController controller = new ListenerTrackingTunerController();
        OverlapSaveChannelSourceManager manager = new OverlapSaveChannelSourceManager(controller);
        long initial = controller.getFrequency();
        long half = TestTunerController.SAMPLE_RATE / 2;
        OverlapSaveChannelSource running = (OverlapSaveChannelSource)source(manager, initial + half - 10000);
        running.start();
        assertEquals(initial, running.getTunerFrequency(), "no retune for a channel inside the band");
        assertEquals(25000.0, running.getSampleRate(), 0.001);

        //The pair spans 2,397,500 Hz of the 2.4 MHz bandwidth, so the tuner retunes 2.5 kHz down and both channels
        //end up 1,250 Hz inside the band edges.  The running channel's 25 kHz output band extends past the edge, which
        //a rule based on the output band would refuse.
        OverlapSaveChannelSource added = (OverlapSaveChannelSource)source(manager, initial - half + 5000);
        long expected = (initial + half - 10000 + 6250 + initial - half + 5000 - 6250) / 2;
        assertEquals(expected, controller.getFrequency(), "tuner centered on the channel set");
        added.start();
        controller.deliver(70000);
        long deadline = System.currentTimeMillis() + 5000;

        while(running.getTunerFrequency() != expected && System.currentTimeMillis() < deadline)
        {
            Thread.sleep(10);
        }

        assertEquals(expected, running.getTunerFrequency(), "running channel follows the retune");
        assertEquals(2, manager.getTunerChannelCount(), "both channels still running");
        running.stop();
        added.stop();
        assertEquals(0, manager.getTunerChannelCount());
    }

    /**
     * A source created before a tuner sample rate change can't use the channelizer for the new rate: its start fails
     * with an error state to its consumer, the tuner doesn't stream for it, and stopping it releases it.
     */
    @Test
    void sampleRateChangeBeforeStartFailsSource() throws Exception
    {
        ListenerTrackingTunerController controller = new ListenerTrackingTunerController();
        OverlapSaveChannelSourceManager manager = new OverlapSaveChannelSourceManager(controller);
        OverlapSaveChannelSource source = (OverlapSaveChannelSource)source(manager, controller.getFrequency() + 100000);
        assertEquals(TestTunerController.SAMPLE_RATE, source.getTunerSampleRate(), 0.5);
        CountDownLatch error = new CountDownLatch(1);
        source.setSourceEventListener(event -> {
            if(event.getEvent() == SourceEvent.Event.NOTIFICATION_ERROR_STATE)
            {
                error.countDown();
            }
        });
        controller.setSampleRate(2000000);
        source.start();
        assertTrue(error.await(5, TimeUnit.SECONDS), "error state sent to the consumer");
        assertFalse(controller.hasBufferListeners(), "tuner doesn't stream for a failed channel");
        source.stop();
        assertEquals(0, manager.getTunerChannelCount());
    }

    /**
     * A sample rate without a whole number of 40 Hz bins with factors 2, 3 and 5 only falls back to polyphase.
     */
    @Test
    void unsupportedSampleRateFallsBackToPolyphase() throws Exception
    {
        ListenerTrackingTunerController controller = new ListenerTrackingTunerController();
        controller.setSampleRate(2800000);
        OverlapSaveChannelSourceManager manager = new OverlapSaveChannelSourceManager(controller);
        TunerChannelSource source = source(manager, controller.getFrequency() + 100000);
        assertInstanceOf(PolyphaseChannelSource.class, source);
        source.start();
        assertEquals(1, manager.getTunerChannelCount());
        assertTrue(controller.isLockedSampleRate());
        source.stop();
        assertEquals(0, manager.getTunerChannelCount());
    }
}
