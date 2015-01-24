package decode.p25.message.tsbk.osp.data;

import alias.AliasList;
import bits.BinaryMessage;
import decode.p25.message.tsbk.UnitChannelGrant;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

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
