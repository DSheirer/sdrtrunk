package decode.p25.message.ldu;

import alias.AliasList;
import bits.BitSetBuffer;
import crc.CRC;
import decode.p25.message.P25Message;
import decode.p25.reference.DataUnitID;

public class LDU1Message extends P25Message
{
	public LDU1Message( BitSetBuffer message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );

	    /* NID CRC is checked in the message framer, thus a constructed message
	     * means it passed the CRC */
	    mCRC = new CRC[ 2 ];
	    mCRC[ 0 ] = CRC.PASSED;
    }
	
	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();
		
		sb.append( "NAC:" );
		sb.append( getNAC() );
		sb.append( " " );
		sb.append( getDUID().getLabel() );
		sb.append( " " );
		
		sb.append( mMessage.toString() );
		
	    return sb.toString();
    }
	
	/* Temporary override */
	public boolean isValid()
	{
		return true;
	}

}
