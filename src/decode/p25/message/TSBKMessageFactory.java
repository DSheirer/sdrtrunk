package decode.p25.message;

import bits.BitSetBuffer;
import decode.p25.reference.Opcode;
import decode.p25.reference.Vendor;

public class TSBKMessageFactory
{
	public static TSBKMessage getMessage( int system, BitSetBuffer buffer )
	{
		int opcode = buffer.getInt( TSBKMessage.OPCODE );

		Vendor vendor = Vendor.fromValue( 
				buffer.getInt( TSBKMessage.VENDOR_ID ) );
		
		switch( vendor )
		{
			case STANDARD:
				switch( Opcode.fromValue( opcode ) )
				{

					
				}
				
				return new TSBKMessage( system, buffer );
			case MOTOROLA:
			default:
				return new TSBKMessage( system, buffer );
		}
	}
}
