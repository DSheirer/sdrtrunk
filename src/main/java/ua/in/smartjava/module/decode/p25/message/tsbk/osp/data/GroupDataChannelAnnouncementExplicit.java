package ua.in.smartjava.module.decode.p25.message.tsbk.osp.data;

import ua.in.smartjava.module.decode.p25.message.tsbk.GroupChannelGrantExplicit;
import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.module.decode.p25.reference.Opcode;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;

public class GroupDataChannelAnnouncementExplicit extends GroupChannelGrantExplicit
{
    public GroupDataChannelAnnouncementExplicit( BinaryMessage message, 
                                   DataUnitID duid,
                                   AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.GROUP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT.getDescription();
    }
}
