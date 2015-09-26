/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
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
package module.decode;

import message.IMessageProvider;
import message.Message;
import module.Module;
import sample.Broadcaster;
import sample.Listener;

public abstract class Decoder extends Module implements IMessageProvider
{
	/* This has to be a broadcaster in order for references to persist */
	protected Broadcaster<Message> mMessageBroadcaster = new Broadcaster<>();

	/**
	 * Decoder - parent class for all decoders, demodulators and components.  
	 */
	public Decoder()
	{
	}

	@Override
	public void dispose()
	{
		mMessageBroadcaster.dispose();
		mMessageBroadcaster = null;
	}

	/**
	 * Identifies the decoder type (ie protocol) 
	 */
	public abstract DecoderType getDecoderType();


    /**
     * Adds a listener for receiving decoded messages from this decoder
     */
    @Override
    public void addMessageListener( Listener<Message> listener )
    {
		mMessageBroadcaster.addListener( listener );
	}

    /**
     * Removes the listener from receiving decoded messages from all attached
     * decoders
     */
    @Override
    public void removeMessageListener( Listener<Message> listener )
    {
		mMessageBroadcaster.removeListener( listener );
    }
}
