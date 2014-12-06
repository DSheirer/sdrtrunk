package decode.p25.message.pdu.osp.data;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.pdu.UnitToUnitChannelGrantExtended;
import decode.p25.message.tsbk.osp.control.IdentifierUpdateReceiver;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

public class IndividualDataChannelGrantExtended 
				extends UnitToUnitChannelGrantExtended 
				implements IdentifierUpdateReceiver
{
	public IndividualDataChannelGrantExtended( BitSetBuffer message,
            DataUnitID duid, AliasList aliasList )
    {
	    super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.INDIVIDUAL_DATA_CHANNEL_GRANT.getDescription();
    }
}
