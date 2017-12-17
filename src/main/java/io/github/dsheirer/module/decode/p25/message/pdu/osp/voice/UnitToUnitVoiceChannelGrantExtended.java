package io.github.dsheirer.module.decode.p25.message.pdu.osp.voice;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.message.pdu.UnitToUnitChannelGrantExtended;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.module.decode.p25.message.IdentifierReceiver;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Opcode;

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
