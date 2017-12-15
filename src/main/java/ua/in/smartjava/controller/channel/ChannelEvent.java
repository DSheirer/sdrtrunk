/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package ua.in.smartjava.controller.channel;

public class ChannelEvent
{
	private Channel mChannel;
	private Event mEvent;

	/**
	 * ChannelEvent - event describing any changes to channels
	 * @param channel - ua.in.smartjava.channel that changed
	 * @param event - change event
	 */
	public ChannelEvent( Channel channel, Event event )
	{
		mChannel = channel;
		mEvent = event;
	}
	
	public Channel getChannel()
	{
		return mChannel;
	}
	
	public Event getEvent()
	{
		return mEvent;
	}
	
	/**
	 * Channel events to describe the specific event
	 */
	public enum Event
	{
		//Channel is added
		NOTIFICATION_ADD,
		//Channel configuration has changed
		NOTIFICATION_CONFIGURATION_CHANGE,
		//Channel is deleted/removed
		NOTIFICATION_DELETE,
		//Channel enable request was rejected
		NOTIFICATION_ENABLE_REJECTED,
		//Channel has started processing/decoding
		NOTIFICATION_PROCESSING_START,
		//Channel has stopped processing/decoding
		NOTIFICATION_PROCESSING_STOP,
		//Channel's selection state has changed
		NOTIFICATION_SELECTION_CHANGE,
		//Channel's ua.in.smartjava.channel-state has been reset
		NOTIFICATION_STATE_RESET,
		//Request to delete the ua.in.smartjava.channel
		REQUEST_DELETE,
		//Request to deselect the ua.in.smartjava.channel
		REQUEST_DESELECT,
		//Request to disable a ua.in.smartjava.channel - response will be a PROCESSING_STOP_NOTIFICATION
		REQUEST_DISABLE,
		//Request to enable a ua.in.smartjava.channel - response will be a PROCESSING_START_NOTIFICATION
		REQUEST_ENABLE,
		//Request to select the ua.in.smartjava.channel
		REQUEST_SELECT;
	}
}
