package decode.p25.message.pdu.osp.voice;

import alias.AliasList;
import bits.BinaryMessage;
import decode.p25.message.IdentifierReceiver;
import decode.p25.message.pdu.UnitToUnitChannelGrantExtended;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

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
