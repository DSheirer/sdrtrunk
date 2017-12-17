package io.github.dsheirer.module.decode.p25.message.pdu.osp.voice;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Opcode;
import io.github.dsheirer.alias.AliasList;

public class TelephoneInterconnectChannelGrantUpdateExplicit 
					extends TelephoneInterconnectChannelGrantExplicit
{	
	public TelephoneInterconnectChannelGrantUpdateExplicit(BinaryMessage message,
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
