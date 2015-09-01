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

import gui.SDRTrunk;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.action.AliasActionManager;
import message.Message;
import sample.Listener;
import controller.ResourceManager;

public class ChannelManager implements ChannelEventListener
{
	private final static Logger mLog = LoggerFactory.getLogger( ChannelManager.class );
	
	private ArrayList<Channel> mChannels = new ArrayList<Channel>();
	
	private ArrayList<ChannelEventListener> mChannelListeners =
									new ArrayList<ChannelEventListener>();
	private ArrayList<Listener<Message>> mMessageListeners =
									new ArrayList<Listener<Message>>();
	
	private AliasActionManager mAliasActionManager;

	public ChannelManager( ResourceManager resourceManager )
	{
		mAliasActionManager = new AliasActionManager( resourceManager );
		
		mChannelListeners.add( this );

		/* Add alias action manager as listener to all channels/messages */
		mMessageListeners.add( mAliasActionManager );
	}

	/**
	 * Notification that a channel has been added or deleted, so that we can
	 * keep track of all channels and manage system-wide listeners for each 
	 * of those channels
	 */
	@SuppressWarnings( "incomplete-switch" )
    @Override
    public void channelChanged( ChannelEvent event )
    {
		switch( event.getEvent() )
		{
			case CHANNEL_ENABLED:
				if( !mChannels.contains( event.getChannel() ) )
				{
					mChannels.add( event.getChannel() );
				}
				break;
			case CHANNEL_DISABLED:
				mChannels.remove( event.getChannel() );
				break;
		}
    }
	
	/**
	 * Adds a channel listener that will be added to all channels to receive
	 * any channel change events.  This listener will automatically receive
	 * a channel add event as it is added to each of the existing channels.
	 */
	public void addListener( ChannelEventListener listener )
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
	public void removeListener( ChannelEventListener listener )
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
	public ArrayList<ChannelEventListener> getChannelListeners()
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
