package decode.p25.message.pdu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.reference.DataUnitID;

public class PDUConfirmedMessage extends PDUMessage
{
	public final static Logger mLog = 
			LoggerFactory.getLogger( PDUConfirmedMessage.class );

	public PDUConfirmedMessage( BitSetBuffer message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );
	    
	    mLog.debug( toString() );
    }

	/* Override for now */
	public boolean isValid()
	{
		return true;
	}

	public String toString()
	{
		return super.toString() + " " + mMessage.toString();
	}
}
