package decode.p25.message.tsbk.osp.voice;

import alias.AliasList;
import bits.BinaryMessage;
import decode.p25.message.tsbk.GroupChannelGrant;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

public class GroupVoiceChannelGrant extends GroupChannelGrant
{
    public GroupVoiceChannelGrant( BinaryMessage message, 
                                   DataUnitID duid,
                                   AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.GROUP_VOICE_CHANNEL_GRANT.getDescription();
    }
    
}
