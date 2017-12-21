package io.github.dsheirer.module.decode.p25.message.pdu.confirmed;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketData extends PDUConfirmedMessage
{
	public final static Logger mLog = 
			LoggerFactory.getLogger( PacketData.class );

	public static final int DATA_BLOCK_START = 176;
	
	public PacketData( BinaryMessage message, AliasList aliasList )
    {
	    super( message, aliasList );
    }

	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "NAC:" );
		sb.append( getNAC() );
		sb.append( " PDUC LLID:" );
		sb.append( getLogicalLinkID() );
		
		
		sb.append( " PACKET DATA" );
		sb.append( " CRC[" );
		sb.append( getErrorStatus() );
		sb.append( "]" );
		
		if( !mMessage.get( FINAL_FRAGMENT_FLAG ) )
		{
			sb.append( " RESENDING" );
		}
		
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
