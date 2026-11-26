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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ChannelHealthTest
{
    static ChannelHealth.ChannelInfo info(int id, String mode)
    {
        return new ChannelHealth.ChannelInfo(id, "Channel " + id, "System", "Site " + id, "List", mode);
    }

    @Test
    void locked_seconds_count_a_good_message_within_the_last_two_seconds()
    {
        ChannelHealth health = new ChannelHealth(info(1, "p25"));
        long t = 1_000_000;

        //A good message every 100 ms for 10 s: locked 10 s.
        for(int i = 0; i <= 100; i++)
        {
            health.control("p25", true, t + i * 100L);
        }

        //A bad one, then nothing for 30 s: the lock holds 2 s past the last good message, no more.
        health.control("p25", false, t + 10_500);
        ChannelHealth.Totals totals = health.totals(t + 40_000);

        assertEquals(10_000 + ChannelHealth.LOCK_HOLD_MS, totals.lockedMs());
        assertEquals(101, totals.good());
        assertEquals(1, totals.bad());
        assertTrue(totals.isSite());
        assertEquals("p25", totals.protocol());

        //Totals only ever grow, whenever they're read.
        assertTrue(health.totals(t + 10_500).lockedMs() <= totals.lockedMs());
    }

    @Test
    void a_conventional_channel_counts_openings_and_seconds_open()
    {
        ChannelHealth health = new ChannelHealth(info(2, "fm"));
        health.started(0);
        health.opened(1_000);
        health.opened(1_500); //Already open: not another opening
        health.closed(4_000);
        health.opened(10_000);

        ChannelHealth.Totals totals = health.totals(12_000);
        assertFalse(totals.isSite());
        assertEquals(2, totals.opens());
        assertEquals(1, totals.closes());
        assertEquals(3_000 + 2_000, totals.openMs());
        assertEquals(12_000, totals.runningMs());

        health.stopped(13_000);
        totals = health.totals(20_000);
        assertEquals(1, totals.closes(), "a transmission cut off by stopping isn't one completed");
        assertEquals(3_000 + 3_000, totals.openMs(), "its time open counts, to the stop");
        assertEquals(13_000, totals.runningMs(), "not running after it stopped");
    }

    /**
     * A DMR Capacity Plus rest channel that moves starts a second chain (and monitor) for the channel before the first,
     * now a traffic channel, stops: the channel runs while either does.
     */
    @Test
    void a_channel_runs_while_any_of_its_monitors_does()
    {
        ChannelHealth health = new ChannelHealth(info(4, "dmr"));
        health.started(0);
        health.started(5_000); //The new rest channel's chain
        health.stopped(6_000); //The old chain, a traffic channel now
        assertEquals(10_000, health.totals(10_000).runningMs(), "still running");

        health.stopped(12_000);
        health.stopped(13_000); //More stops than starts: nothing more
        assertEquals(12_000, health.totals(20_000).runningMs());
        health.started(30_000);
        assertEquals(17_000, health.totals(35_000).runningMs());
    }

    @Test
    void identity_is_what_the_site_says_of_itself()
    {
        ChannelHealth health = new ChannelHealth(info(3, "p25"));
        health.identity("wacn", 781824);
        health.identity("sysId", 939);
        health.identity("nac", 928);
        assertEquals(3, health.totals(0).identity().size());
        assertEquals(939, health.totals(0).identity().get("sysId"));
    }
}
