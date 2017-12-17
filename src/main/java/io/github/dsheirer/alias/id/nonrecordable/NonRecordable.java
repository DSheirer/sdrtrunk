package io.github.dsheirer.alias.id.nonrecordable;

import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;

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
