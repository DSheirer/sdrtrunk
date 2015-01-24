package decode.p25.message.pdu;

import alias.AliasList;
import bits.BinaryMessage;
import decode.p25.message.P25Message;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;
import decode.p25.reference.PDUFormat;
import decode.p25.reference.ServiceAccessPoint;
import decode.p25.reference.Vendor;
import edac.CRC;

public class PDUMessage extends P25Message
{
	public static final int CONFIRMATION_REQUIRED_INDICATOR = 65;
	public static final int PACKET_DIRECTION_INDICATOR = 66;
	public static final int[] FORMAT = { 67,68,69,70,71 };
	public static final int[] SAP_ID = { 74,75,76,77,78,79 };
	public static final int[] VENDOR_ID = { 80,81,82,83,84,85,86,87 };
	public static final int[] LOGICAL_LINK_ID = { 88,89,90,91,92,93,94,95,96,97,
		98,99,100,101,102,103,104,105,106,107,108,109,110,111 };
	public static final int[] BLOCKS_TO_FOLLOW = { 113,114,115,116,117,118,119 };
	public static final int[] PAD_OCTET_COUNT = { 123,124,125,126,127 };
	public static final int[] OPCODE = { 122,123,124,125,126,127 };
	public static final int[] DATA_HEADER_OFFSET = { 138,139,140,141,142,143 };
	public static final int[] PDU_CRC = { 144,145,146,147,148,149,150,151,152,
		153,154,155,156,157,158,159 };

	public PDUMessage( BinaryMessage message, DataUnitID duid, AliasList aliasList )
    {
        super( message, duid, aliasList );

        /* Setup a CRC array to hold the header CRC and the multi-block CRC */
        mCRC = new CRC[ 2 ];
        mCRC[ 0 ] = CRC.PASSED;
    }
	
	protected String getMessageStub()
	{
		StringBuilder sb = new StringBuilder();

		Vendor vendor = getVendor();
		
		sb.append( "NAC:" );
		sb.append( getNAC() );
		sb.append( " " );
		sb.append( getDUID().getLabel() );

		if( vendor == Vendor.STANDARD )
		{
			sb.append( " " );
			sb.append( getOpcode().getLabel() );
		}
		else
		{
			sb.append( " " );
			sb.append( vendor.getLabel() );
		}
		
	    return sb.toString();
	}

	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( getMessageStub() );

		switch( getFormat() )
		{
			case ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL:
				break;
			case UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL:
				sb.append( " PAD OCTETS:" + getPadOctetCount() );
				sb.append( " DATA HDR OFFSET:" + getDataHeaderOffset() );
				break;
			default:
		}

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
		sb.append( " LLID:" );
		sb.append( getLogicalLinkID() );
		sb.append( " BLKS TO FOLLOW:" );
		sb.append( getBlocksToFollowCount() );
		
	    return sb.toString();
    }

	public String getConfirmation()
	{
		return mMessage.get( CONFIRMATION_REQUIRED_INDICATOR ) ? "CONFIRMED" : "UNCONFIRMED";
	}
	
	public String getDirection()
	{
		return mMessage.get( PACKET_DIRECTION_INDICATOR ) ? "OSP" : "ISP";
	}
	
	public boolean isOutbound()
	{
		return mMessage.get( PACKET_DIRECTION_INDICATOR );
	}
	
	public PDUFormat getFormat()
	{
		return PDUFormat.fromValue( mMessage.getInt( FORMAT ) );
	}
	
	public ServiceAccessPoint getServiceAccessPoint()
	{
		return ServiceAccessPoint.fromValue( mMessage.getInt( SAP_ID ) );
	}

	public Vendor getVendor()
	{
		return Vendor.fromValue( mMessage.getInt( VENDOR_ID ) );
	}
	
	public String getLogicalLinkID()
	{
		return mMessage.getHex( LOGICAL_LINK_ID, 6 );
	}
	
	public int getBlocksToFollowCount()
	{
		return mMessage.getInt( BLOCKS_TO_FOLLOW );
	}
	
	public int getPadOctetCount()
	{
		return mMessage.getInt( PAD_OCTET_COUNT );
	}
	
	public Opcode getOpcode()
	{
		if( getFormat() == PDUFormat.ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL )
		{
			return Opcode.fromValue( mMessage.getInt( OPCODE ) );
		}
		
		return Opcode.UNKNOWN;
	}
	
	public int getDataHeaderOffset()
	{
		return mMessage.getInt( DATA_HEADER_OFFSET );
	}
}
