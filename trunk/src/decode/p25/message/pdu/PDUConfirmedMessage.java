package decode.p25.message.pdu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.reference.DataUnitID;
import edac.CRC;
import edac.CRCP25;

public class PDUConfirmedMessage extends PDUMessage
{
	public final static Logger mLog = 
			LoggerFactory.getLogger( PDUConfirmedMessage.class );

	public PDUConfirmedMessage( BitSetBuffer message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );

	    checkCRC();
	    
	    mLog.debug( toString() );
    }
	
	private void checkCRC()
	{
		int blocks = getBlocksToFollowCount();
		
		/* The NID and Header have already passed CRC */
        mCRC = new CRC[ 2 + blocks ];
        mCRC[ 0 ] = CRC.PASSED;
        mCRC[ 1 ] = CRC.PASSED;
		
		for( int x = 0; x < getBlocksToFollowCount(); x++ )
		{
			/* Data blocks start at 160 and every 144 thereafter */
			mCRC[ x + 2 ] = CRCP25.checkCRC9( mMessage, 160 + ( x * 144 ) );
		}
		
		//TODO: check the final 4-byte end of block sequence CRC
	}

	/* Override for now */
	public boolean isValid()
	{
		return true;
	}
	
	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "NAC:" );
		sb.append( getNAC() );
		sb.append( " " );
		sb.append( getDUID().getLabel() );
		
		sb.append( " CRC " );
		sb.append( getErrorStatus() );

		sb.append( " LLID:" );
		sb.append( getLogicalLinkID() );

		sb.append( " " );
		sb.append( getConfirmation() );
		sb.append( " " );
		sb.append( getDirection() );
		sb.append( " FMT:" );
		sb.append( getFormat().getLabel() );
		sb.append( " SAP:" );
		sb.append( getServiceAccessPoint().name() );
		sb.append( " VEND:" );
		sb.append( getVendor().getLabel() );
		sb.append( " BLKS TO FOLLOW:" );
		sb.append( getBlocksToFollowCount() );
		
	    return sb.toString();
    }
	

	public String toString()
	{
		return super.toString() + " " + mMessage.toString();
	}
}
