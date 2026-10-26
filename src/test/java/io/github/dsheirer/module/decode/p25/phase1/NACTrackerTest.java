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

package io.github.dsheirer.module.decode.p25.phase1;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit tests for the NAC tracker.
 */
public class NACTrackerTest
{
    /**
     * Test: the dominant NAC needs the minimum number of observations.
     */
    @Test
    void testDominantNAC()
    {
        NACTracker tracker = new NACTracker();
        assertEquals(0, tracker.getTrackedNAC(), "Empty");
        tracker.track(0x293);
        tracker.track(0x293);
        tracker.track(0x111);
        assertEquals(0, tracker.getTrackedNAC(), "Below the minimum observations");
        tracker.track(0x293);
        assertEquals(0x293, tracker.getTrackedNAC(), "Dominant");
    }

    /**
     * Test: when a fourth NAC is observed, the least recently observed NAC is evicted, independent of the system clock.
     * All of the observations occur within the same millisecond, as they do when a recording decodes faster than real
     * time.
     */
    @Test
    void testLeastRecentlyObservedEvicted()
    {
        for(int repeat = 0; repeat < 100; repeat++)
        {
            NACTracker tracker = new NACTracker();

            //0x293 is the most observed, but the least recently observed when 0x444 arrives
            for(int x = 0; x < 5; x++)
            {
                tracker.track(0x293);
            }

            for(int x = 0; x < 4; x++)
            {
                tracker.track(0x111);
            }

            tracker.track(0x222);
            tracker.track(0x444);
            assertEquals(0x111, tracker.getTrackedNAC(), "Least recently observed NAC evicted, repeat " + repeat);
        }
    }
}
