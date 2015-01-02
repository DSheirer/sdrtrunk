package decode.p25.message.pdu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Vendor;

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
	
	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "NAC:" );
		sb.append( getNAC() );
		sb.append( " " );
		sb.append( getDUID().getLabel() );

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
