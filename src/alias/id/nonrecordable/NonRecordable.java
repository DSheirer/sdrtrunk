package alias.id.nonrecordable;

import alias.id.AliasID;
import alias.id.AliasIDType;

public class NonRecordable extends AliasID
{
	@Override
	public AliasIDType getType()
	{
		return AliasIDType.NON_RECORDABLE;
	}

	@Override
	public boolean isValid()
	{
		return true;
	}

	@Override
	public boolean matches( AliasID id )
	{
		return false;
	}
	
	@Override
	public String toString()
	{
		return "Audio Non-Recordable";
	}
}
