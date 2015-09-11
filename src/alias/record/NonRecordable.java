package alias.record;

import alias.AliasID;
import alias.AliasIDType;

public class NonRecordable extends AliasID
{
	@Override
	public AliasIDType getType()
	{
		return AliasIDType.NonRecordable;
	}

	@Override
	public boolean matches( AliasID id )
	{
		return false;
	}
}
