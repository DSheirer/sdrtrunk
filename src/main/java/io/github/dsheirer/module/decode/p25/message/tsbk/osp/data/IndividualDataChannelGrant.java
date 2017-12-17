package io.github.dsheirer.module.decode.p25.message.tsbk.osp.data;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.module.decode.p25.message.tsbk.UnitChannelGrant;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Opcode;

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
