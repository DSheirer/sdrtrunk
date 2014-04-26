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
package controller.channel;

import java.util.ArrayList;

import message.Message;
import sample.Listener;
import controller.ResourceManager;
import controller.channel.Channel.ChannelEvent;

public class ChannelManager implements ChannelListener
{
	private ArrayList<Channel> mChannels = new ArrayList<Channel>();
	
	private ArrayList<ChannelListener> mChannelListeners =
									new ArrayList<ChannelListener>();
	private ArrayList<Listener<Message>> mMessageListeners =
									new ArrayList<Listener<Message>>();

	public ChannelManager( ResourceManager resourceManager )
	{
		mChannelListeners.add( this );
	}

	/**
	 * Notification that a channel has been added or deleted, so that we can
	 * keep track of all channels and manage system-wide listeners for each 
	 * of those channels
	 */
	@SuppressWarnings( "incomplete-switch" )
    @Override
    public void occurred( Channel channel, ChannelEvent component )
    {
		switch( component )
		{
			case CHANNEL_ADDED:
				if( !mChannels.contains( channel ) )
				{
					mChannels.add( channel );
				}
				break;
			case CHANNEL_DELETED:
				mChannels.remove( channel );
				break;
		}
    }
	
	/**
	 * Adds a channel listener that will be added to all channels to receive
	 * any channel change events.  This listener will automatically receive
	 * a channel add event as it is added to each of the existing channels.
	 */
	public void addListener( ChannelListener listener )
	{
	    mChannelListeners.add( listener );
	    
	    for( Channel channel: mChannels )
	    {
	    	channel.addListener( listener );
	    }
	}

	/**
	 * Removes a channel listener from being automatically added to all channels
	 * to receive channel change events.
	 */
	public void removeListener( ChannelListener listener )
	{
	    mChannelListeners.remove( listener );
	    
	    for( Channel channel: mChannels )
	    {
	    	channel.removeListener( listener );
	    }
	}

	/**
	 * Returns the list of channel change listeners that will be automatically
	 * added to all channels to receive channel change events
	 */
	public ArrayList<ChannelListener> getChannelListeners()
	{
	    return mChannelListeners;
	}

	/**
	 * Returns the list of channel change listeners that will be automatically
	 * added to all channels to receive channel change events
	 */
	public ArrayList<Listener<Message>> getMessageListeners()
	{
	    return mMessageListeners;
	}

	/**
	 * Adds a message listener that will be added to all channels to receive
	 * any messages.
	 */
	public void addListener( Listener<Message> listener )
	{
	    mMessageListeners.add( listener );
	}

	/**
	 * Removes a message listener.
	 */
	public void removeListener( Listener<Message> listener )
	{
	    mMessageListeners.remove( listener );
	}
}
