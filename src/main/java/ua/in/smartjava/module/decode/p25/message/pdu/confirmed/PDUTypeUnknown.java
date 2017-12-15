package ua.in.smartjava.module.decode.p25.message.pdu.confirmed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PDUTypeUnknown extends PDUConfirmedMessage
{
	public final static Logger mLog = 
			LoggerFactory.getLogger( PDUTypeUnknown.class );

	public PDUTypeUnknown( PDUConfirmedMessage message )
    {
	    super( message );
    }

	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "NAC:" );
		sb.append( getNAC() );
		sb.append( " LLID:" );
		sb.append( getLogicalLinkID() );
		sb.append( " PACKET DATA UNIT CONFIRMED - " );
		sb.append( getPDUType().getLabel() );
		sb.append( " CRC[" );
		sb.append( getErrorStatus() );
		sb.append( "]" );
		
		sb.append( " PACKET #" );
		sb.append( getPacketSequenceNumber() );
		
		if( isFinalFragment() && getFragmentSequenceNumber() == 0 )
		{
			sb.append( ".C" );
		}
		else
		{
			sb.append( "." );
			sb.append( getFragmentSequenceNumber() );
			
			if( isFinalFragment() )
			{
				sb.append( "C" );
			}
		}
		
		sb.append( " " );
		
		sb.append( mMessage.toString() );
		
	    return sb.toString();
    }
}
