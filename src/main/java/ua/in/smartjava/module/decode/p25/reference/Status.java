package ua.in.smartjava.module.decode.p25.reference;

public enum Status
{
	TALKAROUND,
	BUSY,
	REPEATER,
	IDLE,
	UNKNOWN;
	
	
	public Status fromValue( int value )
	{
		if( 0 <= value && value <= 3 )
		{
			return values()[ value ];
		}
		
		return UNKNOWN;
	}
}
