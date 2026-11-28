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

package io.github.dsheirer.health;

import java.util.List;

/**
 * Every channel's and tuner's running totals at one moment.  A report's window is the difference of two snapshots.
 * @param timeMs when, Unix milliseconds
 * @param sinceMs when the totals started (the application started), Unix milliseconds
 * @param channels standard channels' totals
 * @param radios tuners' totals
 */
public record HealthSnapshot(long timeMs, long sinceMs, List<ChannelHealth.Totals> channels, List<RadioTotals> radios)
{
    /**
     * A channel's totals in this snapshot, or null when it hadn't started.
     */
    public ChannelHealth.Totals channel(int channelId)
    {
        for(ChannelHealth.Totals totals: channels)
        {
            if(totals.info().channelId() == channelId)
            {
                return totals;
            }
        }

        return null;
    }

    /**
     * A tuner's totals in this snapshot, or null.
     */
    public RadioTotals radio(String name)
    {
        for(RadioTotals totals: radios)
        {
            if(totals.name().equals(name))
            {
                return totals;
            }
        }

        return null;
    }

    /**
     * A tuner's totals.
     * @param name the tuner's name
     * @param kind as a node health report names it (airspy, rtlsdr, ...)
     * @param seconds running since the start
     * @param ppmError frequency error left after correction, as measured by the decoders, or null when not measured
     */
    public record RadioTotals(String name, String kind, double seconds, Double ppmError)
    {
    }
}
