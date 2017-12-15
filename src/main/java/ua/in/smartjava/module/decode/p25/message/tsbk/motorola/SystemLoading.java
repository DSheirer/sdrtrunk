package ua.in.smartjava.module.decode.p25.message.tsbk.motorola;

import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;

public class SystemLoading extends MotorolaTSBKMessage
{
	public SystemLoading( BinaryMessage message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );
    }

	@Override
    public String getEventType()
    {
	    return MotorolaOpcode.SYSTEM_LOAD.getLabel();
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
