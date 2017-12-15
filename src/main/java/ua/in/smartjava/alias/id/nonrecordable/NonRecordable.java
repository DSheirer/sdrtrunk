package ua.in.smartjava.alias.id.nonrecordable;

import ua.in.smartjava.alias.id.AliasID;
import ua.in.smartjava.alias.id.AliasIDType;

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
