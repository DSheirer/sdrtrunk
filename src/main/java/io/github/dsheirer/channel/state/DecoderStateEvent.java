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
        RESET,
        SOURCE_FREQUENCY,
        START;
    }
}