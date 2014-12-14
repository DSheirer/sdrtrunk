package decode.p25.message.tsbk.motorola;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.reference.DataUnitID;

public class SystemLoading extends MotorolaTSBKMessage
{
	public SystemLoading( BitSetBuffer message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );
    }

	@Override
    public String getEventType()
    {
	    return MotorolaOpcode.OP09.getLabel();
    }
	
	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( getMessageStub() );
		
		sb.append( " - " + getMessageHex() );
		
	    return sb.toString();
    }
	
}
