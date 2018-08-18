/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.decode.p25.message.IBandIdentifier;
import io.github.dsheirer.module.decode.p25.message.IdentifierReceiver;
import io.github.dsheirer.module.decode.p25.message.ldu.LDUMessage;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class P25MessageProcessor implements Listener<Message>
{
	private final static Logger mLog = LoggerFactory.getLogger( P25MessageProcessor.class );

	private Listener<Message> mMessageListener;

	/* Map of up to 16 band identifiers per RFSS.  These identifier update 
	 * messages are inserted into any message that conveys channel information
	 * so that the uplink/downlink frequencies can be calculated */
	private HashMap<Integer,IBandIdentifier> mBandIdentifierMap =
			new HashMap<Integer,IBandIdentifier>();
	
	private AliasList mAliasList;
	
	public P25MessageProcessor( AliasList aliasList )
	{
		mAliasList = aliasList;
	}
	
	@Override
    public void receive( Message message )
    {
		/**
		 * Capture frequency band identifier messages and inject them into any
		 * messages that require band information in order to calculate the 
		 * up-link and down-link frequencies for any numeric channel references
		 * contained within the message.
		 */
		if( message.isValid() )
		{
			/* Insert band identifier update messages into channel-type messages */
			if( message instanceof IdentifierReceiver )
			{
				IdentifierReceiver receiver = (IdentifierReceiver)message;
				
				int[] identifiers = receiver.getIdentifiers();
				
				for( int identifier: identifiers )
				{
					receiver.setIdentifierMessage( identifier, 
									mBandIdentifierMap.get( identifier ) );
				}
			}

			/* Store band identifiers so that they can be injected into channel
			 * type messages */
			if( message instanceof IBandIdentifier )
			{
				IBandIdentifier bandIdentifier = (IBandIdentifier)message;
				
				mBandIdentifierMap.put( bandIdentifier.getIdentifier(), 
									bandIdentifier );
			}
		}

		/**
		 * Broadcast all valid messages and any LDU voice messages regardless if
		 * they are valid or not, so that we don't miss any voice frames
		 */
		if( mMessageListener != null && message.isValid() || message instanceof LDUMessage)
		{
			mMessageListener.receive( message );
		}
    }
	
	public void dispose()
	{
		mBandIdentifierMap.clear();
		mMessageListener = null;
	}
	
    public void setMessageListener( Listener<Message> listener )
    {
    	mMessageListener = listener;
    }

    public void removeMessageListener()
    {
    	mMessageListener = null;
    }
}
