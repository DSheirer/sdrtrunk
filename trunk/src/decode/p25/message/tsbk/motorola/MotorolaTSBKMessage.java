package decode.p25.message.tsbk.motorola;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.reference.DataUnitID;

public class MotorolaTSBKMessage extends TSBKMessage
{
	public MotorolaTSBKMessage( BitSetBuffer message, DataUnitID duid,
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

		sb.append( " MOTOROLA" );

		if( isEncrypted() )
		{
			sb.append( " ENCRYPTED" );
		}
		
		sb.append( " OPCD:" );
		sb.append( getMotorolaOpcode().getLabel() );
		
	    return sb.toString();
	}

	public MotorolaOpcode getMotorolaOpcode()
	{
		return MotorolaOpcode.fromValue( mMessage.getInt( OPCODE ) );
	}
}
