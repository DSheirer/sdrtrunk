package io.github.dsheirer.module.decode.p25.message.tsbk.osp.data;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.module.decode.p25.message.tsbk.GroupMultiChannelGrant;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Opcode;

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
