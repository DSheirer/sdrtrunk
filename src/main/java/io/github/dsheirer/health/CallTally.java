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

import java.util.EnumMap;
import java.util.Map;

/**
 * Running totals of the calls a control channel announced and what became of each, since the channel started.
 *
 * A call's outcome can change while it lasts: a grant refused because every traffic channel was busy can be followed
 * on a later update, and a traffic channel request can be rejected afterwards because no tuner covers the frequency.
 * Each call therefore keeps its current outcome (see ChannelEventTracker.tally) and moves it here, so every call is
 * counted once, by what finally became of it.
 *
 * Thread safe.
 */
public class CallTally
{
    private long mStarted;
    private final Map<CallOutcome,Long> mOutcomes = new EnumMap<>(CallOutcome.class);

    /**
     * Moves a call from one outcome to another.
     * @param previous outcome of the call, or null when the call is new (counted as started)
     * @param next outcome of the call
     */
    public synchronized void count(CallOutcome previous, CallOutcome next)
    {
        if(previous == next || next == null)
        {
            return;
        }

        if(previous == null)
        {
            mStarted++;
        }
        else
        {
            mOutcomes.merge(previous, -1L, Long::sum);
        }

        mOutcomes.merge(next, 1L, Long::sum);
    }

    /**
     * Calls announced so far.
     */
    public synchronized long getStarted()
    {
        return mStarted;
    }

    /**
     * Calls with the outcome, so far.
     */
    public synchronized long get(CallOutcome outcome)
    {
        return mOutcomes.getOrDefault(outcome, 0L);
    }

    /**
     * Copy of the totals: started and each outcome.
     */
    public synchronized Totals totals()
    {
        return new Totals(mStarted, new EnumMap<>(mOutcomes.isEmpty() ? new EnumMap<>(CallOutcome.class) : mOutcomes));
    }

    /**
     * Call totals at one moment.
     * @param started calls announced
     * @param outcomes calls by outcome
     */
    public record Totals(long started, Map<CallOutcome,Long> outcomes)
    {
        public static final Totals EMPTY = new Totals(0, new EnumMap<>(CallOutcome.class));

        public long get(CallOutcome outcome)
        {
            return outcomes.getOrDefault(outcome, 0L);
        }
    }
}
