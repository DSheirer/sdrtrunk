package module.decode.p25;

import java.util.HashMap;

import message.Message;
import module.decode.p25.message.IBandIdentifier;
import module.decode.p25.message.IdentifierReceiver;
import module.decode.p25.message.ldu.LDUMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.AliasList;
import sample.Listener;

public class P25MessageProcessor implements Listener<Message>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( P25MessageProcessor.class );

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
