/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
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
package controller.state;

import message.Message;
import sample.Broadcaster;
import sample.Listener;
import controller.state.ChannelState.ChangedAttribute;

/**
 * ChannelState for auxiliary decoders.
 */
public abstract class AuxChannelState implements Listener<Message>
{
	protected ChannelState mParentChannelState;
	protected Broadcaster<ChangedAttribute> mChangeBroadcaster =
			new Broadcaster<ChangedAttribute>();
	
	public AuxChannelState( ChannelState parentState )
	{
		mParentChannelState = parentState;
	}
	
	/**
	 * Prepare for garbage collect
	 */
	public void dispose()
	{
		mChangeBroadcaster.dispose();
		mParentChannelState = null;
	}
	
	/**
	 * Call fade - change display and await a call to reset()
	 */
	public abstract void fade();
	
	/**
	 * Activity summary for this decoder
	 */
	public abstract String getActivitySummary();
	
	/**
	 * Call end - cleanup the display
	 */
	public abstract void reset();

	public void addListener( Listener<ChangedAttribute> listener )
	{
		mChangeBroadcaster.addListener( listener );
	}
	
	public void removeListener( Listener<ChangedAttribute> listener )
	{
		mChangeBroadcaster.removeListener( listener );
	}
	
	public void broadcastChange( ChangedAttribute attribute )
	{
		if( mChangeBroadcaster != null )
		{
			mChangeBroadcaster.broadcast( attribute );
		}
	}
}
