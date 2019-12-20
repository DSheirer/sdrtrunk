/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.audio.squelch;

/**
 * Squelch state change event
 */
public class SquelchStateEvent
{
    private SquelchState mSquelchState;
    private int mTimeslot;

    public SquelchStateEvent(SquelchState squelchState, int timeslot)
    {
        mSquelchState = squelchState;
        mTimeslot = timeslot;
    }

    public SquelchState getSquelchState()
    {
        return mSquelchState;
    }

    /**
     * Timeslot associated with the squelch state
     */
    public int getTimeslot()
    {
        return mTimeslot;
    }

    /**
     * Creates a squelch state event with a default timeslot of 0
     * @param squelchState for the event
     * @return event
     */
    public static SquelchStateEvent create(SquelchState squelchState)
    {
        return new SquelchStateEvent(squelchState, 0);
    }

    /**
     * Creates a squelch state event for the specified timeslot
     * @param squelchState for the event
     * @param timeslot that the event applies to
     * @return event
     */
    public static SquelchStateEvent create(SquelchState squelchState, int timeslot)
    {
        return new SquelchStateEvent(squelchState, timeslot);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Squelch Event - State:").append(mSquelchState).append(" Timeslot:").append(mTimeslot);
        return sb.toString();
    }
}
