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

/**
 * State and Status request and notification events used for communication between Decoder States and Channel states
 * and by peripheral entities like the ChannelRotationMonitor.
 */
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

    /**
     * Creates a decoder state notification event for a null source with the specified state and timeslot.
     * @param state (current) of the decoder
     * @param timeslot that is reporting state
     * @return new decoder state event
     */
    public static DecoderStateEvent stateNotification(State state, int timeslot)
    {
        return new DecoderStateEvent(null, Event.NOTIFICATION_CHANNEL_STATE, state, timeslot);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Decoder State Event - source[").append(mSource != null ? mSource.getClass() : "null")
                .append("] event[").append(mEvent)
                .append("] state[").append(mState)
                .append("] timeslot[").append(mTimeslot)
                .append("] frequency [").append(mFrequency).append("]");

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
        //Decode state discrete events
        CONTINUATION,
        DECODE,
        END,
        START,

        NOTIFICATION_CHANNEL_STATE,
        NOTIFICATION_CHANNEL_ACTIVE_STATE,
        NOTIFICATION_CHANNEL_INACTIVE_STATE,
        NOTIFICATION_SOURCE_FREQUENCY,


        REQUEST_ALWAYS_UNSQUELCH,
        REQUEST_CHANGE_CALL_TIMEOUT,
        REQUEST_RESET,

        //Request to convert the current channel to a traffic channel (used by DMR Cap+)
        REQUEST_CONVERT_TO_TRAFFIC_CHANNEL;
    }
}