package io.github.dsheirer.module.decode.p25.reference;

public enum StackOperation
{
	CLEAR,
	WRITE,
	DELETE,
	READ,
	UNKNOWN;
	
	public static StackOperation fromValue( int value )
	{
		if( 0 <= value && value <= 3 )
		{
			return values()[ value ];
		}
		
		return UNKNOWN;
	}
}
