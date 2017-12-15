/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2015 Dennis Sheirer
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
package controller.channel;

import controller.channel.ChannelEvent.Event;

public class ChannelSelectionManager implements ChannelEventListener
{
	private IChannelEventBroadcaster mChannelEventBroadcaster;
	private Channel mSelectedChannel = null;

	/**
	 * Manages channel selection state to ensure that only one channel is ever
	 * in a selected state.
	 */
	public ChannelSelectionManager( IChannelEventBroadcaster broadcaster )
	{
		mChannelEventBroadcaster = broadcaster;
	}

	@Override
	public void channelChanged( ChannelEvent event )
	{
		switch( event.getEvent() )
		{
			case REQUEST_SELECT:
				if( mSelectedChannel != null )
				{
					mSelectedChannel.setSelected( false );
					mChannelEventBroadcaster.broadcast( new ChannelEvent( mSelectedChannel, 
							   Event.NOTIFICATION_SELECTION_CHANGE ) );
					mSelectedChannel = null;
				}
				
				mSelectedChannel = event.getChannel();
				mSelectedChannel.setSelected( true );
				mChannelEventBroadcaster.broadcast( new ChannelEvent( event.getChannel(), 
						   Event.NOTIFICATION_SELECTION_CHANGE ) );
				break;
			case NOTIFICATION_PROCESSING_STOP:
			case REQUEST_DESELECT:
				if( mSelectedChannel != null && 
					mSelectedChannel == event.getChannel() )
				{
					mSelectedChannel.setSelected( false );
					mSelectedChannel = null;
					mChannelEventBroadcaster.broadcast( new ChannelEvent( event.getChannel(), 
							   Event.NOTIFICATION_SELECTION_CHANGE ) );
				}
				break;
			default:
				break;
		}
	}

}
