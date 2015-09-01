package module.decode.p25.message.tsbk.motorola;

import module.decode.p25.reference.DataUnitID;
import alias.AliasList;
import bits.BinaryMessage;

public class PatchGroupDelete extends PatchGroup
{
	public PatchGroupDelete( BinaryMessage message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );
    }

	@Override
    public String getEventType()
    {
	    return MotorolaOpcode.PATCH_GROUP_DELETE.getLabel();
    }
}	
