package decode.p25.message.tsbk;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

public class UnitToUnitVoiceChannelGrantUpdate extends UnitChannelGrant
{
    public UnitToUnitVoiceChannelGrantUpdate( BitSetBuffer message, 
            DataUnitID duid, AliasList aliasList )
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE.getDescription();
    }
}
