package decode.p25.message.tsbk.osp.voice;

import alias.AliasList;
import bits.BinaryMessage;
import decode.p25.message.tsbk.UnitChannelGrant;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

public class UnitToUnitAnswerRequest extends UnitChannelGrant
{
    public UnitToUnitAnswerRequest( BinaryMessage message, 
                                   DataUnitID duid,
                                   AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.UNIT_TO_UNIT_ANSWER_REQUEST.getDescription();
    }
}
