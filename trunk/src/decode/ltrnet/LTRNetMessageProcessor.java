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
package decode.ltrnet;

import java.util.HashMap;

import message.Message;
import message.MessageDirection;
import message.MessageType;
import sample.Broadcaster;
import sample.Listener;
import alias.AliasList;
import bits.BinaryMessage;

public class LTRNetMessageProcessor implements Listener<BinaryMessage>
{
	private Broadcaster<Message> mBroadcaster = new Broadcaster<Message>();

	private HashMap<Integer,LTRNetOSWMessage> mReceiveHighAuxMessages =
			new HashMap<Integer,LTRNetOSWMessage>();
	private HashMap<Integer,LTRNetOSWMessage> mReceiveLowAuxMessages =
			new HashMap<Integer,LTRNetOSWMessage>();

	private HashMap<Integer,LTRNetOSWMessage> mTransmitHighAuxMessages =
			new HashMap<Integer,LTRNetOSWMessage>();
	private HashMap<Integer,LTRNetOSWMessage> mTransmitLowAuxMessages =
			new HashMap<Integer,LTRNetOSWMessage>();

	private LTRNetISWMessage mESNHighMessage;
	private LTRNetISWMessage mESNLowMessage;
	
	private MessageDirection mDirection;
	private AliasList mAliasList;
	
	public LTRNetMessageProcessor( MessageDirection direction, 
								   AliasList list )
	{
		mDirection = direction;
		mAliasList = list;
	}
	
	@Override
    public void receive( BinaryMessage buffer )
    {
		LTRNetMessage message;
		
		if( mDirection == MessageDirection.OSW )
		{
			message = new LTRNetOSWMessage( buffer, mAliasList );
			
			if( message.isValid() )
			{
				if( message.getMessageType() == MessageType.FQ_TXHI )
				{
					mTransmitHighAuxMessages.put( message.getHomeRepeater(), 
												  (LTRNetOSWMessage)message );
					
					message.setAuxiliaryMessage( mTransmitLowAuxMessages.get( 
							message.getHomeRepeater() ) );
				}
				else if( message.getMessageType() == MessageType.FQ_TXLO )
				{
					mTransmitLowAuxMessages.put( message.getHomeRepeater(), 
											  (LTRNetOSWMessage)message );

					message.setAuxiliaryMessage( mTransmitHighAuxMessages.get( 
							message.getHomeRepeater() ) );
				}
				else if( message.getMessageType() == MessageType.FQ_RXHI )
				{
					mReceiveHighAuxMessages.put( message.getHomeRepeater(), 
							  (LTRNetOSWMessage)message );

					message.setAuxiliaryMessage( mReceiveLowAuxMessages.get( 
							message.getHomeRepeater() ) );
				}
				else if( message.getMessageType() == MessageType.FQ_RXLO )
				{
					mReceiveLowAuxMessages.put( message.getHomeRepeater(), 
											(LTRNetOSWMessage)message );
					message.setAuxiliaryMessage( mReceiveHighAuxMessages.get( 
							message.getHomeRepeater() ) );
				}
			}
		}
		else
		{
			buffer.flip( 0, 40 );

			message = new LTRNetISWMessage( buffer, mAliasList );

			if( message.isValid() )
			{
				/**
				 * Catch ESN High messages to marry up with ESN Low messages.  Purge
				 * the ESN high message after 2 messages, so that it doesn't get
				 * paired with the wrong esn low message
				 */
				if( message.getMessageType() == MessageType.ID_ESNH )
				{
					mESNHighMessage = (LTRNetISWMessage)message;

					if( mESNLowMessage != null )
					{
						/* Check that the stored message is not older than 
						 * 5 seconds */
						if( System.currentTimeMillis() - 
								mESNLowMessage.getTimeReceived() < 5000 )
						{
							((LTRNetISWMessage)message).setAuxiliaryMessage( mESNLowMessage );
						}
						else
						{
							mESNLowMessage = null;
						}
					}
				}
				else if( message.getMessageType() == MessageType.ID_ESNL )
				{
					mESNLowMessage = (LTRNetISWMessage)message;
					
					if( mESNHighMessage != null )
					{
						/* Check that the stored message is not older than 5 seconds */
						if( System.currentTimeMillis() - mESNHighMessage.getTimeReceived() < 5000 )
						{
							((LTRNetISWMessage)message).setAuxiliaryMessage( mESNHighMessage );
						}
						else
						{
							mESNHighMessage = null;
						}
					}
				}
			}
		}
		
		mBroadcaster.receive( message );
    }
	
    public void addMessageListener( Listener<Message> listener )
    {
		mBroadcaster.addListener( listener );
    }

    public void removeMessageListener( Listener<Message> listener )
    {
    	mBroadcaster.removeListener( listener );
    }
}
