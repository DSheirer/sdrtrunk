package ua.in.smartjava.module.decode.p25.message.vselp;

import ua.in.smartjava.module.decode.p25.message.P25Message;
import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;

public class VSELP1Message extends P25Message
{
	/**
	 * Motorola VSELP ua.in.smartjava.audio ua.in.smartjava.message 1.
	 * 
	 * @param message
	 * @param duid
	 * @param aliasList
	 */
	public VSELP1Message( BinaryMessage message, DataUnitID duid, AliasList aliasList )
	{
		super( message, duid, aliasList );
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

		sb.append( "VSELP 1 VOICE FRAME" );

	    return sb.toString();
    }
}
