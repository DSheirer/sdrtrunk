package decode.p25.message.tsbk;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.P25Message;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;
import decode.p25.reference.Vendor;

public class TSBKMessage extends P25Message
{
	public static final int LAST_BLOCK_FLAG = 64;
	public static final int ENCRYPTED_FLAG = 65;
	public static final int[] OPCODE = { 66,67,68,69,70,71 };
	public static final int[] VENDOR_ID = { 72,73,74,75,76,77,78,79 };
    public static final int[] CRC = { 144,145,146,147,148,149,150,151,152,153,
        154,155,156,157,158,159 };

    public TSBKMessage( BitSetBuffer message, DataUnitID duid, AliasList aliasList )
    {
	    super( message, duid, aliasList );
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
