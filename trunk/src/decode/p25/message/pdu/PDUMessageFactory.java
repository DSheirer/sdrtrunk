package decode.p25.message.pdu;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;
import decode.p25.reference.PDUFormat;
import decode.p25.reference.Vendor;

public class PDUMessageFactory
{
	public static PDUMessage getMessage( BitSetBuffer message, 
										 DataUnitID duid,
										 AliasList aliasList )
	{
		PDUFormat format = PDUFormat.fromValue( 
					message.getInt( PDUMessage.FORMAT ) );

		switch( format )
		{
			case ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL:
				Vendor vendor = Vendor.fromValue( 
						message.getInt( PDUMessage.VENDOR_ID ) );
				
				switch( vendor )
				{
					case STANDARD:
						Opcode opcode = Opcode.fromValue( 
								message.getInt( PDUMessage.OPCODE ) );
						
						switch( opcode )
						{
							case NETWORK_STATUS_BROADCAST:
								return new NetworkStatusBroadcastExtended( 
										message, duid, aliasList );
							case RFSS_STATUS_BROADCAST:
								return new RFSSStatusBroadcastExtended( message, 
										duid, aliasList );
							default:
								break;
						}
					case MOTOROLA:
						break;
					default:
						break;
				}
				
				return new PDUMessage( message, duid, aliasList );
				
			case UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL:
				return new PDUMessage( message, duid, aliasList );
				
			default:
				return new PDUMessage( message, duid, aliasList );
		}
	}
}
