package decode.p25.message.tdu;

import alias.AliasList;
import bits.BitSetBuffer;
import crc.CRC;
import decode.p25.message.P25Message;
import decode.p25.reference.DataUnitID;

public class TDUMessage extends P25Message
{
	public TDUMessage( BitSetBuffer message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );

	    /* NID CRC is checked in the message framer, thus a constructed message
	     * means it passed the CRC */
	    mCRC = new CRC[ 1 ];
	    mCRC[ 0 ] = CRC.PASSED;
    }
}
