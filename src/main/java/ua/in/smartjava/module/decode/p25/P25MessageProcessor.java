package ua.in.smartjava.module.decode.p25;

import java.util.HashMap;

import ua.in.smartjava.message.Message;
import ua.in.smartjava.module.decode.p25.message.IBandIdentifier;
import ua.in.smartjava.module.decode.p25.message.IdentifierReceiver;
import ua.in.smartjava.module.decode.p25.message.ldu.LDUMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.sample.Listener;

public class P25MessageProcessor implements Listener<Message>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( P25MessageProcessor.class );

	private Listener<Message> mMessageListener;

	/* Map of up to 16 band identifiers per RFSS.  These identifier update 
	 * messages are inserted into any ua.in.smartjava.message that conveys ua.in.smartjava.channel information
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
		 * up-link and down-link frequencies for any numeric ua.in.smartjava.channel references
		 * contained within the ua.in.smartjava.message.
		 */
		if( message.isValid() )
		{
			/* Insert band identifier update messages into ua.in.smartjava.channel-type messages */
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

			/* Store band identifiers so that they can be injected into ua.in.smartjava.channel
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
		if( mMessageListener != null && message.isValid() || message instanceof LDUMessage )
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
