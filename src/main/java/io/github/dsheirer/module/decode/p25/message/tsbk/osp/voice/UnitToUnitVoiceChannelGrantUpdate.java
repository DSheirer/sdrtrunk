package io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.UnitChannelGrant;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.message.tsbk2.Opcode;

public class UnitToUnitVoiceChannelGrantUpdate extends UnitChannelGrant
{
    public UnitToUnitVoiceChannelGrantUpdate( BinaryMessage message, 
            DataUnitID duid, AliasList aliasList )
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE.toString();
    }
}
