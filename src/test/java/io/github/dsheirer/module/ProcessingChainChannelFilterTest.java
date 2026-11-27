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
package io.github.dsheirer.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.tuner.channel.MultiFrequencyTunerChannelSource;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The processing chain's startup sample rate event carries the tuner channel source's channel filter flag to the
 * modules, and a multiple frequency source never claims it.
 */
public class ProcessingChainChannelFilterTest
{
    private static final double SAMPLE_RATE = 25000.0;

    /**
     * Tuner channel source that reports a fixed channel filter flag.
     */
    private static class TestSource extends TunerChannelSource
    {
        private final boolean mChannelFiltered;

        TestSource(boolean channelFiltered)
        {
            super(null, new TunerChannel(450000000, 12500), "test channel", null);
            mChannelFiltered = channelFiltered;
        }

        @Override
        public boolean isChannelFiltered()
        {
            return mChannelFiltered;
        }

        @Override
        public void setFrequencyCorrection(long correction)
        {
        }

        @Override
        public void setFrequency(long frequency)
        {
        }

        @Override
        protected void setSampleRate(double sampleRate)
        {
        }

        @Override
        public void setListener(Listener<ComplexSamples> complexSamplesListener)
        {
        }

        @Override
        public double getSampleRate()
        {
            return SAMPLE_RATE;
        }
    }

    /**
     * Module that records the source events broadcast to the chain's modules.
     */
    private static class Recorder extends Module implements ISourceEventListener
    {
        private final List<SourceEvent> mEvents = new ArrayList<>();

        @Override
        public Listener<SourceEvent> getSourceEventListener()
        {
            return mEvents::add;
        }

        @Override
        public void reset()
        {
        }

        @Override
        public void start()
        {
        }

        @Override
        public void stop()
        {
        }

        SourceEvent getSampleRateChange()
        {
            List<SourceEvent> events = mEvents.stream()
                .filter(event -> event.getEvent() == SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE).toList();
            assertEquals(1, events.size(), "startup sample rate events");
            return events.get(0);
        }
    }

    private static SourceEvent start(TunerChannelSource source)
    {
        ProcessingChain chain = new ProcessingChain(new Channel("test"), new AliasModel());
        Recorder recorder = new Recorder();
        chain.addModule(recorder);
        chain.setSource(source);

        try
        {
            chain.start();
            return recorder.getSampleRateChange();
        }
        finally
        {
            chain.stop();
            chain.dispose();
        }
    }

    @Test
    void startupEventCarriesTheFlag()
    {
        SourceEvent filtered = start(new TestSource(true));
        assertEquals(SAMPLE_RATE, filtered.getValue().doubleValue());
        assertEquals(true, filtered.isChannelFiltered());

        SourceEvent unfiltered = start(new TestSource(false));
        assertEquals(SAMPLE_RATE, unfiltered.getValue().doubleValue());
        assertFalse(unfiltered.isChannelFiltered());
    }

    /**
     * A multiple frequency source can rotate to a source that doesn't filter without telling the consumer, so it
     * doesn't claim the flag even when its current source filters.
     */
    @Test
    void multipleFrequencySourceNeverClaimsTheFlag()
    {
        TestSource inner = new TestSource(true);
        MultiFrequencyTunerChannelSource source = new MultiFrequencyTunerChannelSource(null, inner,
            List.of(450000000L, 451000000L), new ChannelSpecification(25000.0, 12500, 6000.0, 7000.0), null, "test");
        assertFalse(source.isChannelFiltered());
        assertFalse(start(source).isChannelFiltered());
    }
}
