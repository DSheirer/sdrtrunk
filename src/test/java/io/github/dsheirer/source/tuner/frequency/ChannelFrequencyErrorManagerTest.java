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

package io.github.dsheirer.source.tuner.frequency;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit tests for the channel frequency error manager.
 */
public class ChannelFrequencyErrorManagerTest
{
    /**
     * Test: a tuner PPM change is removed from the channel's applied frequency correction, and the updated correction
     * is applied to the channel source.
     */
    @Test
    void testTunerPPMChange()
    {
        TestChannelSource source = new TestChannelSource();
        ChannelFrequencyErrorManager manager = new ChannelFrequencyErrorManager(source, null);

        manager.receive(-10);
        assertEquals(10, manager.getFrequencyError(), "Applied correction");
        assertEquals(10, source.mFrequencyCorrection, "Channel source correction");

        manager.receive(25);
        assertEquals(-15, manager.getFrequencyError(), "Applied correction");
        assertEquals(-15, source.mFrequencyCorrection, "Channel source correction");
    }

    /**
     * Channel source that records the applied frequency correction.
     */
    private static class TestChannelSource extends TunerChannelSource
    {
        private long mFrequencyCorrection = Long.MIN_VALUE;

        public TestChannelSource()
        {
            super(null, new TunerChannel(460_000_000L, 12_500), "test", null);
        }

        @Override
        public void setFrequencyCorrection(long correction)
        {
            mFrequencyCorrection = correction;
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
            return 50_000.0;
        }

        @Override
        public long getFrequency()
        {
            return 460_000_000L;
        }

        @Override
        public void reset()
        {
        }
    }
}
