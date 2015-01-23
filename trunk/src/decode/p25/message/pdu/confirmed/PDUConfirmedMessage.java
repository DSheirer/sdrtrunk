package decode.p25.message.pdu.confirmed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.pdu.PDUMessage;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.PDUType;
import decode.p25.reference.Vendor;
import edac.CRC;
import edac.CRCP25;

public class PDUConfirmedMessage extends PDUMessage
{
	public final static Logger mLog = 
			LoggerFactory.getLogger( PDUConfirmedMessage.class );

	public static final int SEQUENCE_RESET_FLAG = 128; //SYN
	public static final int[] PACKET_SEQUENCE_NUMBER = { 129,130,131 };
	public static final int FINAL_FRAGMENT_FLAG = 132;
	public static final int[] FRAGMENT_SEQUENCE_NUMBER = { 133,134,135 };
	public static final int[] PDU_TYPE = { 176,177,178,179 };
	
	public PDUConfirmedMessage( BitSetBuffer message, AliasList aliasList )
    {
	    super( message, DataUnitID.PDUC, aliasList );

	    checkCRC();
    }

	/**
	 * Constructor to support subclassing of this message.  Initial construction
	 * of the message enables CRC checks to be calculated and message type to
	 * be inspected so that we can construct an accurate subclass message,
	 * without having to recalculate CRC checksums.
	 */
	protected PDUConfirmedMessage( PDUConfirmedMessage message )
	{
		super( message.getSourceMessage(), DataUnitID.PDUC, message.getAliasList() );
		mCRC = message.getCRCResults();
		
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

		sb.append( "[" + this.getClass() + "]" );
		sb.append( "NAC:" );
		sb.append( getNAC() );
		sb.append( " " );
		sb.append( getDUID().getLabel() );
		sb.append( " " );
		sb.append( getDirection() );
		sb.append( " CRC[" );
		sb.append( getErrorStatus() );
		sb.append( "] LLID:" );
		sb.append( getLogicalLinkID() );

		sb.append( " " );
		sb.append( getFormat().getLabel() );
		sb.append( " SAP:" );
		sb.append( getServiceAccessPoint().getLabel() );
		
		if( getVendor() != Vendor.STANDARD )
		{
			sb.append( " VEND:" );
			sb.append( getVendor().getLabel() );
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
		
		sb.append( " BLOCKS:" );
		sb.append( getBlocksToFollowCount() );
		
		sb.append( " " );
		sb.append( mMessage.toString() );
		
	    return sb.toString();
    }

	/**
	 * Packet Data Unit Type
	 */
	public PDUType getPDUType()
	{
		return PDUType.fromValue( mMessage.getInt( PDU_TYPE ), isOutbound() );
	}

	
	
	/**
	 * Number of bytes/octets contained in the message, exclusive of the data
	 * header.
	 */
	public int getOctetCount()
	{
		/* 16 octets per block, and 12 octets in final block */
		int count = getBlocksToFollowCount() * 16 - 4;

		int padding = getPadOctetCount();
		
		/* Hack: motorola sndcp packet data control - when it says there are 15
		 * pad octets and only 1 data block of 12 data octets, that means there 
		 * are more pad octets than available data and they appear to suppress 
		 * the final padded data block containing the 12 pad octets. The data
		 * block count should be 2, but it says 1, so they're either not sending
		 * the final block, or there is an error in the block count */
		if( padding > count )
		{
			count -= ( padding - 12 );
		}
		else
		{
			count -= padding;
		}
		
		
		count -= getDataHeaderOffset();
		
		return count;
	}

	/**
	 * Indicates that the receiver should accept this packet, despite the 
	 * apparent out-of-order packet and fragment sequence numbering.  This 
	 * indicates that something happened to the synchronization between
	 * the tower and the radio and now the tower commands the radio to reset
	 * the sequence numbers and accept this packet/fragment instead of discarding
	 * it as a duplicate.
	 */
	public boolean isSequenceReset()
	{
		return mMessage.get( SEQUENCE_RESET_FLAG );
	}

	/**
	 * Packet sequence number.  Numbers run from 0 to 7 and will continually 
	 * roll-over if there are more than 8 total packets.
	 */
	public int getPacketSequenceNumber()
	{
		return mMessage.getInt( PACKET_SEQUENCE_NUMBER );
	}

	/**
	 * Indicates that this packet fragment is the final fragment
	 */
	public boolean isFinalFragment()
	{
		return mMessage.get( FINAL_FRAGMENT_FLAG );
	}
	
	/**
	 * Packet fragment sequence number
	 */
	public int getFragmentSequenceNumber()
	{
		return mMessage.getInt( FRAGMENT_SEQUENCE_NUMBER );
	}
	
	public String toString()
	{
		return getMessage();
	}
}
