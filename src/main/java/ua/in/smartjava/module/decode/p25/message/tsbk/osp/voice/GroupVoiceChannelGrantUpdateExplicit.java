package ua.in.smartjava.module.decode.p25.message.tsbk.osp.voice;

import ua.in.smartjava.module.decode.p25.message.tsbk.GroupChannelGrantExplicit;
import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.module.decode.p25.reference.Opcode;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;

public class GroupVoiceChannelGrantUpdateExplicit extends GroupChannelGrantExplicit
{
    public GroupVoiceChannelGrantUpdateExplicit( BinaryMessage message, 
                                   DataUnitID duid,
                                   AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT.getDescription();
    }
}
