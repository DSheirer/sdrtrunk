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

import io.github.dsheirer.module.decode.event.ChannelEventTracker;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import org.junit.jupiter.api.Test;

public class CallTallyTest
{
    private static class Tracker extends ChannelEventTracker<DecodeEvent>
    {
        Tracker()
        {
            super(DecodeEvent.builder(DecodeEventType.CALL_GROUP, 0).build());
        }
    }

    @Test
    void each_call_is_counted_once_by_what_finally_became_of_it()
    {
        CallTally tally = new CallTally();

        //Refused while every traffic channel was busy, then followed on a later update.
        Tracker rescued = new Tracker();
        rescued.tally(tally, CallOutcome.NO_RECORDER);
        rescued.tally(tally, CallOutcome.NO_RECORDER);
        rescued.tally(tally, CallOutcome.RECORDED);

        //Followed, then the tuner request was rejected.
        Tracker rejected = new Tracker();
        rejected.tally(tally, CallOutcome.RECORDED);
        rejected.tally(tally, CallOutcome.NO_SOURCE);

        //Ignored, encrypted.
        Tracker encrypted = new Tracker();
        encrypted.tally(tally, CallOutcome.ENCRYPTED);

        //Not counted: no tally given.
        new Tracker().tally(null, CallOutcome.RECORDED);

        assertEquals(3, tally.getStarted());
        assertEquals(1, tally.get(CallOutcome.RECORDED));
        assertEquals(0, tally.get(CallOutcome.NO_RECORDER));
        assertEquals(1, tally.get(CallOutcome.NO_SOURCE));
        assertEquals(1, tally.get(CallOutcome.ENCRYPTED));

        long outcomes = 0;

        for(CallOutcome outcome: CallOutcome.values())
        {
            outcomes += tally.get(outcome);
        }

        assertEquals(tally.getStarted(), outcomes, "every call has exactly one outcome");
    }
}
