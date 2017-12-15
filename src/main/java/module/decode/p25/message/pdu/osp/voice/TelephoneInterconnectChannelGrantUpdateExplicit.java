package module.decode.p25.message.pdu.osp.voice;

import module.decode.p25.reference.DataUnitID;
import module.decode.p25.reference.Opcode;
import alias.AliasList;
import bits.BinaryMessage;

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
