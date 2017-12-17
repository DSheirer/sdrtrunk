package io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.GroupChannelGrantExplicit;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Opcode;

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
