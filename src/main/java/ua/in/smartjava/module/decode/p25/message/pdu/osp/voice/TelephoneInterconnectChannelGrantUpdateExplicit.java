package ua.in.smartjava.module.decode.p25.message.pdu.osp.voice;

import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.module.decode.p25.reference.Opcode;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;

public class TelephoneInterconnectChannelGrantUpdateExplicit 
					extends TelephoneInterconnectChannelGrantExplicit
{	
	public TelephoneInterconnectChannelGrantUpdateExplicit( BinaryMessage message,
            DataUnitID duid, AliasList aliasList )
    {
	    super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE.getDescription();
    }
}
