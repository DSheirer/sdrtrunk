package ua.in.smartjava.module.decode.p25.message.tsbk.osp.data;

import ua.in.smartjava.module.decode.p25.message.tsbk.UnitChannelGrant;
import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.module.decode.p25.reference.Opcode;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;

public class IndividualDataChannelGrant extends UnitChannelGrant
{
    public IndividualDataChannelGrant( BinaryMessage message, 
                                   DataUnitID duid,
                                   AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.INDIVIDUAL_DATA_CHANNEL_GRANT.getDescription();
    }
}
