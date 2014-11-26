package decode.p25;

import java.util.Arrays;
import java.util.HashMap;

import message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bits.BitSetBuffer;
import sample.Broadcaster;
import sample.Listener;
import alias.AliasList;
import decode.mpt1327.MPT1327Message;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.message.tsbk.osp.control.IdentifierUpdate;
import decode.p25.message.tsbk.osp.control.IdentifierUpdateReceiver;

public class P25MessageProcessor implements Listener<Message>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( P25MessageProcessor.class );

	private Broadcaster<Message> mBroadcaster = new Broadcaster<Message>();

	/* Map of up to 16 band identifiers per RFSS.  These identifier update 
	 * messages are inserted into any message that conveys channel information
	 * so that the uplink/downlink frequencies can be calculated */
	private HashMap<Integer,IdentifierUpdate> mIdentifierMap = 
			new HashMap<Integer,IdentifierUpdate>();
	
	private AliasList mAliasList;
	
	public P25MessageProcessor( AliasList aliasList )
	{
		mAliasList = aliasList;
	}
	
	@Override
    public void receive( Message message )
    {
		/* Insert band identifier update messages into channel-type messages */
		if( message instanceof IdentifierUpdateReceiver )
		{
			IdentifierUpdateReceiver receiver = (IdentifierUpdateReceiver)message;
			
			int[] identifiers = receiver.getIdentifiers();
			
			for( int identifier: identifiers )
			{
				receiver.setIdentifierMessage( identifier, 
								mIdentifierMap.get( identifier ) );
			}
		}

		/* Store band identifiers so that they can be injected into channel
		 * type messages */
		if( message instanceof IdentifierUpdate )
		{
			IdentifierUpdate identifierUpdate = (IdentifierUpdate)message;
			
			mIdentifierMap.put( identifierUpdate.getIdentifier(), identifierUpdate );
		}
		
//		if( message instanceof TSBKMessage )
//		{
//			mLog.debug( message.getMessage() );
//		}
//		else
//		{
//			mLog.debug( message.getMessage() + "\t" + message.getBinaryMessage() );
//		}
		
		mBroadcaster.broadcast( message );
    }
	
	public void dispose()
	{
		mIdentifierMap.clear();
		mBroadcaster.dispose();
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
