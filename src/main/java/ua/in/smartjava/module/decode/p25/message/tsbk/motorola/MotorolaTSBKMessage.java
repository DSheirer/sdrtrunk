package ua.in.smartjava.module.decode.p25.message.tsbk.motorola;

import ua.in.smartjava.module.decode.p25.message.tsbk.TSBKMessage;
import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;

public class MotorolaTSBKMessage extends TSBKMessage
{
	public MotorolaTSBKMessage( BinaryMessage message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );
    }

	protected String getMessageStub()
	{
		StringBuilder sb = new StringBuilder();

		sb.append( "NAC:" );
		sb.append( getNAC() ); /* NAC is the system id for TSBK messages */
		sb.append( " " );
		sb.append( getDUID().getLabel() );

		sb.append( " MOTOROLA " );

		if( isEncrypted() )
		{
			sb.append( " ENCRYPTED " );
		}
		
		sb.append( getMotorolaOpcode().getLabel() );
		
	    return sb.toString();
	}

	public MotorolaOpcode getMotorolaOpcode()
	{
		return MotorolaOpcode.fromValue( mMessage.getInt( OPCODE ) );
	}
}
