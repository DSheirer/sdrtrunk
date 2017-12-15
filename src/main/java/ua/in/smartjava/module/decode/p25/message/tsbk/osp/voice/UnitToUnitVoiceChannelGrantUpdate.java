package ua.in.smartjava.module.decode.p25.message.tsbk.osp.voice;

import ua.in.smartjava.module.decode.p25.message.tsbk.UnitChannelGrant;
import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.module.decode.p25.reference.Opcode;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;

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
        return Opcode.UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE.getDescription();
    }
}
