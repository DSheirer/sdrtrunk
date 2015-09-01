package module.decode.p25.reference;

public enum Response
{
	ACCEPT, 
	FAIL, 
	DENY, 
	REFUSED,
	UNKNOWN;
	
	public static Response fromValue( int value )
	{
		if( 0 <= value && value <= 4 )
		{
			return values()[ value ];
		}
		
		return UNKNOWN;
	}
}
