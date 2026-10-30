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

import io.github.dsheirer.source.tuner.channel.TunerChannel;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the selection of one or two polyphase channel indexes for a tuner channel.
 */
public class ChannelCalculatorTest
{
    private static final long CENTER = 100_000_000L;
    private static final double SAMPLE_RATE = 2_400_000.0;
    private static final int CHANNEL_COUNT = 96;
    private static final int CHANNEL_BANDWIDTH = 25000;

    private static List<Integer> indexes(int channelCenterOffset, int frequencyOffset, int bandwidth)
    {
        return calculator().getChannelIndexes(new TunerChannel(frequency(channelCenterOffset, frequencyOffset), bandwidth));
    }

    private static long frequency(int channelCenterOffset, int frequencyOffset)
    {
        return CENTER + (long)channelCenterOffset * CHANNEL_BANDWIDTH + frequencyOffset;
    }

    private static ChannelCalculator calculator()
    {
        return new ChannelCalculator(SAMPLE_RATE, CHANNEL_COUNT, CENTER, 2.0);
    }

    /**
     * Test: a narrow channel stays on one index until its band edge reaches the channelizer roll-off.
     */
    @Test
    void narrowChannelMovesToTwoIndexesBeforeBandEdgeRollOff()
    {
        assertEquals(List.of(5), indexes(5, 2500, 12500));
        assertEquals(List.of(5, 6), indexes(5, 3500, 12500));
        assertEquals(List.of(4, 5), indexes(5, -3500, 12500));
        assertEquals(List.of(5), indexes(5, 6000, 5000));
    }

    /**
     * Test: a wide channel near a polyphase channel center stays on one index, otherwise it uses the nearest boundary.
     */
    @Test
    void wideChannelUsesOneIndexNearChannelCenter()
    {
        assertEquals(List.of(5), indexes(5, 0, 25000));
        assertEquals(List.of(5), indexes(5, 1000, 25000));
        assertEquals(List.of(5, 6), indexes(5, 3500, 25000));
        assertEquals(List.of(4, 5), indexes(5, -3500, 25000));
        assertEquals(List.of(5, 6), indexes(5, 12500, 25000));
    }

    /**
     * Test: index pairs are ordered low to high in frequency across the center and wrap around indexes.
     */
    @Test
    void indexPairsAreOrderedAcrossCenterAndWrapAround()
    {
        assertEquals(List.of(95, 0), indexes(0, -6000, 12500));
        assertEquals(List.of(0, 1), indexes(0, 6000, 12500));
        assertEquals(List.of(47, 48), indexes(47, 12500, 12500));
        assertEquals(List.of(48, 49), indexes(-47, -12500, 12500));
    }

    /**
     * Test: every sourceable channel across the spectrum uses at most two adjacent indexes and the synthesized center
     * frequency is the boundary nearest to the channel.
     */
    @Test
    void allChannelsUseAtMostTwoIndexesAroundNearestBoundary()
    {
        ChannelCalculator calculator = calculator();

        for(int bandwidth : new int[]{3000, 6250, 12500, 15000, 25000})
        {
            long start = CENTER - (long)(SAMPLE_RATE / 2) + bandwidth;
            long stop = CENTER + (long)(SAMPLE_RATE / 2) - bandwidth;

            for(long frequency = start; frequency <= stop; frequency += 730)
            {
                TunerChannel channel = new TunerChannel(frequency, bandwidth);
                List<Integer> indexes;

                try
                {
                    indexes = calculator.getChannelIndexes(channel);
                }
                catch(IllegalArgumentException iae)
                {
                    continue; //Within the wrap around channel, which can't be sourced
                }

                assertTrue(indexes.size() <= 2, "Too many indexes " + indexes + " for " + channel);

                long error = Math.abs(calculator.getCenterFrequencyForIndexes(indexes) - frequency);
                assertTrue(error <= CHANNEL_BANDWIDTH / 2 + 1, "Center " + error + " Hz from " + channel);

                if(indexes.size() == 2)
                {
                    assertEquals((indexes.get(0) + 1) % CHANNEL_COUNT, indexes.get(1), "Not adjacent: " + indexes);
                }
            }
        }
    }

    /**
     * Test: channels outside of the tuner bandwidth are still rejected.
     */
    @Test
    void channelBeyondTunerBandwidthIsRejected()
    {
        assertThrows(IllegalArgumentException.class, () -> calculator().getChannelIndexes(
            new TunerChannel(CENTER + (long)(SAMPLE_RATE / 2), 12500)));
    }
}
