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

package io.github.dsheirer.channel.state;

public class DecoderStateEvent
{
    private static final DecoderStateEvent ACTIVE_STATE_TIMESLOT_0 =
        new DecoderStateEvent(null, Event.NOTIFICATION_CHANNEL_ACTIVE_STATE, State.ACTIVE, 0);
    private static final DecoderStateEvent ACTIVE_STATE_TIMESLOT_1 =
        new DecoderStateEvent(null, Event.NOTIFICATION_CHANNEL_ACTIVE_STATE, State.ACTIVE, 1);
    private static final DecoderStateEvent INACTIVE_STATE_TIMESLOT_0 =
        new DecoderStateEvent(null, Event.NOTIFICATION_CHANNEL_INACTIVE_STATE, State.ACTIVE, 0);
    private static final DecoderStateEvent INACTIVE_STATE_TIMESLOT_1 =
        new DecoderStateEvent(null, Event.NOTIFICATION_CHANNEL_INACTIVE_STATE, State.ACTIVE, 1);
    private Object mSource;
    private Event mEvent;
    private State mState;
    private int mTimeslot;
    private long mFrequency;

    public DecoderStateEvent(Object source, Event event, State state, int timeslot, long frequency)
    {
        mSource = source;
        mEvent = event;
        mState = state;
        mTimeslot = timeslot;
        mFrequency = frequency;
    }

    public DecoderStateEvent(Object source, Event event, State state, int timeslot)
    {
        this(source, event, state, timeslot, 0l);
    }

    public DecoderStateEvent(Object source, Event event, State state)
    {
        this(source, event, state, 0, 0l);
    }

    public DecoderStateEvent(Object source, Event event, State state, long frequency)
    {
        this(source, event, state, 0, frequency);
    }

    /**
     * Creates a channel state active event for the specified timeslot
     * @param timeslot that the state applies to
     * @return new event
     */
    public static DecoderStateEvent activeState(int timeslot)
    {
        if(timeslot == 0)
        {
            return ACTIVE_STATE_TIMESLOT_0;
        }
        else if(timeslot == 1)
        {
            return ACTIVE_STATE_TIMESLOT_1;
        }
        else
        {
            throw new IllegalArgumentException("Only timeslots 0 and 1 are currently supported");
        }
    }

    /**
     * Creates a channel state active event for the specified timeslot.
     * @param state currently
     * @return new event
     */
    public static DecoderStateEvent activeState()
    {
        return activeState(0);
    }

    /**
     * Creates a channel state inactive event for the specified timeslot
     * @param timeslot that the state applies to
     * @return new event
     */
    public static DecoderStateEvent inactiveState(int timeslot)
    {
        if(timeslot == 0)
        {
            return INACTIVE_STATE_TIMESLOT_0;
        }
        else if(timeslot == 1)
        {
            return INACTIVE_STATE_TIMESLOT_1;
        }
        else
        {
            throw new IllegalArgumentException("Only timeslots 0 and 1 are currently supported");
        }
    }

    /**
     * Creates a channel state active event for the specified timeslot.
     * @param state currently
     * @return new event
     */
    public static DecoderStateEvent inactiveState()
    {
        return inactiveState(0);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Decoder State Event - source[" + mSource.getClass() +
            "] event[" + mEvent.toString() +
            "] state[" + mState.toString() +
            "] timeslot[" + mTimeslot +
            "] frequency [" + mFrequency + "]");

        return sb.toString();
    }

    public Object getSource()
    {
        return mSource;
    }

    public Event getEvent()
    {
        return mEvent;
    }

    public State getState()
    {
        return mState;
    }

    public long getFrequency()
    {
        return mFrequency;
    }

    public int getTimeslot()
    {
        return mTimeslot;
    }

    public enum Event
    {
        ALWAYS_UNSQUELCH,
        CHANGE_CALL_TIMEOUT,
        CONTINUATION,
        DECODE,
        END,
        NOTIFICATION_CHANNEL_ACTIVE_STATE,
        NOTIFICATION_CHANNEL_INACTIVE_STATE,
        RESET,
        SOURCE_FREQUENCY,
        START;
    }
}