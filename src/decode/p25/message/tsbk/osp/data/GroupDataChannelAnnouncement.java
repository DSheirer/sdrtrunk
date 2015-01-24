package decode.p25.message.tsbk.osp.data;

import alias.AliasList;
import bits.BinaryMessage;
import decode.p25.message.tsbk.GroupMultiChannelGrant;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

public class GroupDataChannelAnnouncement extends GroupMultiChannelGrant
{
    public GroupDataChannelAnnouncement( BinaryMessage message, 
                                   DataUnitID duid,
                                   AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.GROUP_DATA_CHANNEL_ANNOUNCEMENT.getDescription();
    }
}
