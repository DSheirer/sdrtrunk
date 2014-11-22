package decode.p25;

import message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import alias.AliasList;
import decode.p25.message.tsbk.TSBKMessage;

public class P25MessageProcessor implements Listener<Message>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( P25MessageProcessor.class );

	private AliasList mAliasList;
	
	public P25MessageProcessor( AliasList aliasList )
	{
		mAliasList = aliasList;
	}
	
	@Override
    public void receive( Message message )
    {
		if( message instanceof TSBKMessage )
		{
			mLog.debug( message.getMessage() );
		}
		else
		{
			mLog.debug( message.getMessage() + "\t" + message.getBinaryMessage() );
		}
    }
}
