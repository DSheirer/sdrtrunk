package decode.p25.message;

import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;
import decode.p25.reference.Vendor;
import bits.BitSetBuffer;

public class TSBKMessage extends P25Message
{
	public static final int LAST_BLOCK_FLAG = 0;
	public static final int ENCRYPTED_FLAG = 1;
	public static final int[] OPCODE = { 2,3,4,5,6,7 };
	public static final int[] VENDOR_ID = { 8,9,10,11,12,13,14,15 };
	
	public TSBKMessage( int system, BitSetBuffer message )
    {
	    super( message, DataUnitID.TSBK1 );
    }
	
	public boolean isLastBlock()
	{
		return mMessage.get( LAST_BLOCK_FLAG );
	}
	
	public boolean isEncrypted()
	{
		return mMessage.get( ENCRYPTED_FLAG );
	}
	
	public Vendor getVendor()
	{
		return Vendor.fromValue( mMessage.getInt( VENDOR_ID ) );
	}
	
	public Opcode getOpcode()
	{
		return Opcode.fromValue( mMessage.getInt( OPCODE ) );
	}
	
	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();
		
		sb.append( "SYSTEM:" );
		sb.append( getNAC() ); /* NAC is the system id for TSBK messages */
		sb.append( " VEND:" );
		sb.append( getVendor().getLabel() );
		
		if( isEncrypted() )
		{
			sb.append( " ENCRYPTED" );
		}
		else
		{
			Opcode opcode = getOpcode();
			
			sb.append( " OPCODE:" );
			sb.append( opcode.name() );
			sb.append( " [" );
			sb.append( mMessage.getHex( OPCODE, 2 ) );
			sb.append( "]" );
		}
		
	    return sb.toString();
    }
}
