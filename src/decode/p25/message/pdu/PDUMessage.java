package decode.p25.message.pdu;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.P25Message;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.PDUFormat;
import decode.p25.reference.ServiceAccessPoint;
import decode.p25.reference.Vendor;

public class PDUMessage extends P25Message
{
	public static final int PDU_HEADER_DUMMY1 = 66; //Always 0
	public static final int PDU_HEADER_CONFIRMATION_REQUIRED = 67;
	public static final int PDU_HEADER_DIRECTION_FLAG = 68;
	public static final int[] PDU_HEADER_FORMAT = { 69,70,71,72,73 };
	public static final int PDU_HEADER_DUMMY2 = 74; //Always 1
	public static final int PDU_HEADER_DUMMY3 = 75; //Always 1
	public static final int[] PDU_HEADER_SAP_ID = { 76,77,78,79,80 };
	public static final int[] PDU_HEADER_VENDOR_ID = { 81,82,83,84,85,86,87,88 };
	public static final int[] PDU_HEADER_LOGICAL_LINK_ID = { 89,90,91,92,93,94,
		97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114 };
	public static final int PDU_HEADER_FULL_MESSAGE_FLAG = 115;
	public static final int[] PDU_HEADER_BLOCKS_TO_FOLLOW = { 116,117,118,119,
		120,121,122 };
	public static final int PDU_HEADER_DUMMY4 = 123; //Always 0
	public static final int PDU_HEADER_DUMMY5 = 124; //Always 0
	public static final int PDU_HEADER_DUMMY6 = 125; //Always 0
	public static final int[] PDU_HEADER_PAD_BLOCKS = { 126,127,128,129,130 };
	public static final int PDU_HEADER_RESYNCHRONIZE_FLAG = 131;
	public static final int[] PDU_HEADER_SEQUENCE_NUMBER = { 132,133,134 };
	public static final int[] PDU_HEADER_FRAGMENT_SEQUENCE_NUMBER = { 135,136,
		137,138 };
	public static final int PDU_HEADER_DUMMY7 = 139; //Always 0
	public static final int PDU_HEADER_DUMMY8 = 140; //Always 0
	public static final int[] PDU_HEADER_DATA_HEADER_OFFSET = { 141,142,143,
		144,145,146 };
	public static final int[] PDU_HEADER_CRC = { 147,148,149,150,151,152,153,154,
		155,156,157,158,159,160,161,162 };

	//status 166,167
	public PDUMessage( BitSetBuffer message, DataUnitID duid, AliasList aliasList )
    {
        super( message, duid, aliasList );
    }
	
	public String getConfirmation()
	{
		return mMessage.get( PDU_HEADER_CONFIRMATION_REQUIRED ) ? "CON" : "UNC";
	}
	
	public String getDirection()
	{
		return mMessage.get( PDU_HEADER_DIRECTION_FLAG ) ? "OSP" : "ISP";
	}
	
	public PDUFormat getFormat()
	{
		return PDUFormat.fromValue( mMessage.getInt( PDU_HEADER_FORMAT ) );
	}
	
	public ServiceAccessPoint getServiceAccessPoint()
	{
		return ServiceAccessPoint.fromValue( mMessage.getInt( PDU_HEADER_SAP_ID ) );
	}

	public Vendor getVendor()
	{
		return Vendor.fromValue( mMessage.getInt( PDU_HEADER_VENDOR_ID ) );
	}
	
	public String getLogicalLinkID()
	{
		return mMessage.getHex( PDU_HEADER_LOGICAL_LINK_ID, 6 );
	}
	
	public boolean isFullMessage()
	{
		return mMessage.get( PDU_HEADER_FULL_MESSAGE_FLAG );
	}
	
	public int getBlockCount()
	{
		return mMessage.getInt( PDU_HEADER_BLOCKS_TO_FOLLOW );
	}
	
	public int getPadBlockCount()
	{
		return mMessage.getInt( PDU_HEADER_PAD_BLOCKS );
	}

	public boolean isResync()
	{
		return mMessage.get( PDU_HEADER_RESYNCHRONIZE_FLAG );
	}
	
	public int getSequenceNumber()
	{
		return mMessage.getInt( PDU_HEADER_SEQUENCE_NUMBER );
	}
	
	public int getFragmentSequenceNumber()
	{
		return mMessage.getInt( PDU_HEADER_FRAGMENT_SEQUENCE_NUMBER );
	}
	
	public int getDataHeaderOffset()
	{
		return mMessage.getInt( PDU_HEADER_DATA_HEADER_OFFSET );
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
		sb.append( getConfirmation() );
		sb.append( " " );
		sb.append( getDirection() );
		sb.append( " FMT:" );
		sb.append( getFormat().getLabel() );
		
		switch( getFormat() )
		{
			case ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL:
			case UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL:
				sb.append( " SAP:" );
				sb.append( getServiceAccessPoint().getLabel() );
				sb.append( " VEND:" );
				sb.append( getVendor().getLabel() );
				sb.append( " LLID:" );
				sb.append( getLogicalLinkID() );
				sb.append( " FMF:" );
				sb.append( isFullMessage() ? "1" : "0" );
				sb.append( " BLKS:" );
				sb.append( getBlockCount() );
				sb.append( " PAD:" );
				sb.append( getPadBlockCount() );
				sb.append( " RESYNC:" );
				sb.append( isResync() ? "1" : "0" );
				sb.append( " SEQ:" );
				sb.append( getSequenceNumber() );
				sb.append( " FRAG:" );
				sb.append( getFragmentSequenceNumber() );
				sb.append( " DHO:" );
				sb.append( getDataHeaderOffset() );
				break;
			default:
		}
		
	    return sb.toString();
    }

}
