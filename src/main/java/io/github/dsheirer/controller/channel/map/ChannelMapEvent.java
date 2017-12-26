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
package io.github.dsheirer.controller.channel.map;

public class ChannelMapEvent
{
	private ChannelMap mChannelMap;
	private Event mEvent;
	private String mPreviousChannelMapName;

	/**
	 * ChannelMapEvent - event describing any changes to a channel map
	 * 
	 * @param channelMap that changed
	 * @param event describing the change
	 */
	public ChannelMapEvent( ChannelMap channelMap, Event event )
	{
		mChannelMap = channelMap;
		mEvent = event;
	}
	
	/**
	 * ChannelMapEvent - channel map renaming event.
	 * 
	 * @param channelMap that changed
	 * @param previousName of the channel map
	 */
	public ChannelMapEvent( ChannelMap channelMap, String previousName )
	{
		this( channelMap, Event.RENAME );

		mPreviousChannelMapName = previousName;
	}
	
	public ChannelMap getChannelMap()
	{
		return mChannelMap;
	}
	
	public String getPreviousChannelMapName()
	{
		return mPreviousChannelMapName;
	}
	
	public Event getEvent()
	{
		return mEvent;
	}

	/**
	 * Events
	 */
	public enum Event
	{
		ADD,
		CHANGE,
		RENAME,
		DELETE;
	}
}
