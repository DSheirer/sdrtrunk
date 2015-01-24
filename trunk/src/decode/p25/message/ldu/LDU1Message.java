package decode.p25.message.ldu;

import alias.AliasList;
import bits.BinaryMessage;
import decode.p25.P25Interleave;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.LinkControlOpcode;
import decode.p25.reference.Vendor;
import decode.p25.reference.VendorLinkControlOpcode;
import edac.CRC;

public class LDU1Message extends LDUMessage
{
	public static final int ENCRYPTION_FLAG = 352;
	public static final int IMPLICIT_VENDOR_FLAG = 353;
	public static final int[] OPCODE = { 354,355,356,357,362,363 };
	public static final int[] VENDOR = { 364,365,366,367,372,373,374,375 };
	
//	public static final int[] OCTET_1 = { 364,365,366,367,372,373,374,375 };
//	public static final int[] OCTET_2 = { 376,377,382,383,384,385,386,387 };
//	public static final int[] OCTET_3 = { 536,537,538,539,540,541,546,547 };
//	public static final int[] OCTET_4 = { 548,549,550,551,556,557,558,559 };
//	public static final int[] OCTET_5 = { 560,561,566,567,568,569,570,571 };
//	public static final int[] OCTET_6 = { 720,721,722,723,724,725,730,731 };
//	public static final int[] OCTET_7 = { 732,733,734,735,740,741,742,743 };
//	public static final int[] OCTET_8 = { 744,745,750,751,752,753,754,755 };
	
	public LDU1Message( BinaryMessage message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );

	    
	    /* NID CRC is checked in the message framer, thus a constructed message
	     * means it passed the CRC */
	    mCRC = new CRC[ 2 ];
	    mCRC[ 0 ] = CRC.PASSED;
	    
	    checkCRC();
    }

	/**
	 * Subclass constructor from an existing LDU1Message
	 */
	protected LDU1Message( LDU1Message message )
	{
		super( message.getSourceMessage(), DataUnitID.LDU1, message.getAliasList() );
		
		mCRC = message.mCRC;
	}
	
	private void checkCRC()
	{
		
	}
	
	@Override
	public String getMessage()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getMessageStub() );
		sb.append( " " );
		sb.append( mMessage.toString() );
		
		return sb.toString();
	}
	
    public String getMessageStub()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( super.getMessageStub() );
		
		if( isEncrypted() )
		{
			sb.append( "ENCRYPTED" );
		}
		else
		{
			if( isImplicitFormat() || getVendor() == Vendor.STANDARD )
			{
				sb.append( getOpcode().getLabel() );
			}
			else
			{
				sb.append( "VENDOR:" + getVendor().getLabel() + " " );
				sb.append( getVendorOpcode().getLabel() );
			}
		}

	    return sb.toString();
    }

	public boolean isEncrypted()
	{
		return mMessage.get( ENCRYPTION_FLAG );
	}
	
	public boolean isImplicitFormat()
	{
		return mMessage.get( IMPLICIT_VENDOR_FLAG );
	}
	
	public LinkControlOpcode getOpcode()
	{
		return LinkControlOpcode.fromValue( mMessage.getInt( OPCODE ) );
	}

	public VendorLinkControlOpcode getVendorOpcode()
	{
		return VendorLinkControlOpcode.fromValue( mMessage.getInt( OPCODE ) );
	}
	
	public Vendor getVendor()
	{
		if( isImplicitFormat() )
		{
			return Vendor.STANDARD;
		}
		
		return Vendor.fromValue( mMessage.getInt( VENDOR ) );
	}
}
