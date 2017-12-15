/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package ua.in.smartjava.channel.traffic;

import ua.in.smartjava.channel.state.DecoderStateEvent;
import ua.in.smartjava.channel.state.State;
import ua.in.smartjava.module.decode.event.CallEvent;

/**
 * Traffic ua.in.smartjava.channel allocation event wraps a call event indicating the ua.in.smartjava.channel
 * and frequency for a traffic ua.in.smartjava.channel allocation event, so that the traffic
 * ua.in.smartjava.channel manager can respond and allocate a decoder ua.in.smartjava.channel and ua.in.smartjava.source if
 * available, or change the wrapped call event to a call detect event and log it.
 */
public class TrafficChannelAllocationEvent extends DecoderStateEvent
{
	private CallEvent mCallEvent;
	
	public TrafficChannelAllocationEvent( Object source, CallEvent callEvent )
	{
		super( source, Event.TRAFFIC_CHANNEL_ALLOCATION, State.CALL );
		mCallEvent = callEvent;
	}
	
	public CallEvent getCallEvent()
	{
		return mCallEvent;
	}
}
