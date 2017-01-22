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
package channel.traffic;

import channel.state.DecoderStateEvent;
import channel.state.State;
import module.decode.event.CallEvent;

/**
 * Traffic channel allocation event wraps a call event indicating the channel
 * and frequency for a traffic channel allocation event, so that the traffic
 * channel manager can respond and allocate a decoder channel and source if 
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
