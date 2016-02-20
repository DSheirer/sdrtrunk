package module.decode.p25.message.tdu;

import module.decode.p25.message.P25Message;
import module.decode.p25.reference.DataUnitID;
import alias.AliasList;
import bits.BinaryMessage;
import edac.CRC;

public class TDUMessage extends P25Message
{
	public TDUMessage( BinaryMessage message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );

	    /* NID CRC is checked in the message framer, thus a constructed message
	     * means it passed the CRC */
	    mCRC = new CRC[ 1 ];
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
		
		sb.append( " TERMINATOR DATA UNIT" );
		
	    return sb.toString();
    }
}
