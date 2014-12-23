package decode.p25.message.hdu;

import alias.AliasList;
import bits.BitSetBuffer;
import crc.CRC;
import decode.p25.message.P25Message;
import decode.p25.reference.DataUnitID;

public class HDUMessage extends P25Message
{
	public HDUMessage( BitSetBuffer message, DataUnitID duid,
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
