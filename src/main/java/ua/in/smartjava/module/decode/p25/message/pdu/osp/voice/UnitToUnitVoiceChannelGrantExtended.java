package ua.in.smartjava.module.decode.p25.message.pdu.osp.voice;

import ua.in.smartjava.module.decode.p25.message.IdentifierReceiver;
import ua.in.smartjava.module.decode.p25.message.pdu.UnitToUnitChannelGrantExtended;
import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.module.decode.p25.reference.Opcode;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;

public class UnitToUnitVoiceChannelGrantExtended 
				extends UnitToUnitChannelGrantExtended 
				implements IdentifierReceiver
{
	public UnitToUnitVoiceChannelGrantExtended( BinaryMessage message,
            DataUnitID duid, AliasList aliasList )
    {
	    super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.UNIT_TO_UNIT_VOICE_CHANNEL_GRANT.getDescription();
    }
}
