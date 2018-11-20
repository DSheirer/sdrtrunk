/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.channel.traffic;

import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.module.decode.event.DecodeEvent;

/**
 * Traffic channel allocation event wraps a call event indicating the channel
 * and frequency for a traffic channel allocation event, so that the traffic
 * channel manager can respond and allocate a decoder channel and source if
 * available, or change the wrapped call event to a call detect event and log it.
 */
public class TrafficChannelAllocationEvent extends DecoderStateEvent
{
    private DecodeEvent mCallEvent;

    public TrafficChannelAllocationEvent(Object source, DecodeEvent callEvent)
    {
        super(source, Event.TRAFFIC_CHANNEL_ALLOCATION, State.CALL);
        mCallEvent = callEvent;
    }

    public DecodeEvent getCallEvent()
    {
        return mCallEvent;
    }
}
