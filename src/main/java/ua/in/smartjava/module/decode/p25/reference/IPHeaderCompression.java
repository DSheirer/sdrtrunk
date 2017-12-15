package ua.in.smartjava.module.decode.p25.reference;

public enum IPHeaderCompression
{
	NONE( 0, "NONE" ),
	RFC1144_COMPRESSED( 1, "RFC-1144 COMPRESSED" ),
	RFC1144_UNCOMPRESSED( 2, "RFC-1144 UNCOMPRESSED" ),
	UNKNOWN( -1, "UNKNOWN" );
	
	private int mValue;
	private String mLabel;
	
	private IPHeaderCompression( int value, String label )
	{
		mValue = value;
		mLabel = label;
	}
	
	public int getValue()
	{
		return mValue;
	}
	
	public String getLabel()
	{
		return mLabel;
	}
	
	public static IPHeaderCompression fromValue( int value )
	{
		if( 0 <= value && value <= 2 )
		{
			return values()[ value ];
		}
		
		return IPHeaderCompression.UNKNOWN;
	}
}
