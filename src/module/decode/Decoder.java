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
package module.decode;

import message.IMessageProvider;
import message.Message;
import module.Module;
import sample.Listener;

public abstract class Decoder extends Module implements IMessageProvider, Listener<Message>
{
	/* This has to be a broadcaster in order for references to persist */
	protected Listener<Message> mMessageListener;

	/**
	 * Decoder - parent class for all decoders, demodulators and components.  
	 */
	public Decoder()
	{
	}

	@Override
	public void dispose()
	{
		mMessageListener = null;
	}

	/**
	 * Identifies the decoder type (ie protocol) 
	 */
	public abstract DecoderType getDecoderType();


    /**
     * Adds a listener for receiving decoded messages from this decoder
     */
    @Override
    public void setMessageListener( Listener<Message> listener )
    {
    	mMessageListener = listener;
	}

    /**
     * Removes the listener from receiving decoded messages from all attached
     * decoders
     */
    @Override
    public void removeMessageListener()
    {
    	mMessageListener = null;
    }

    /**
     * Broadcasts the message to the registered message listener
     */
    public void broadcast( Message message )
    {
    	if( mMessageListener != null )
    	{
    		mMessageListener.receive( message );
    	}
    }

	@Override
	public void receive( Message message )
	{
		broadcast( message );
	}

	@Override
	public void reset()
	{
	}

	@Override
	public void start()
	{
	}

	@Override
	public void stop()
	{
	}
}
