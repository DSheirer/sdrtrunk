package decode.p25.message.tsbk.motorola;

public enum MotorolaOpcode
{
	UNKNOWN_OPCODE_5( "MOTO OPCODE 05  ", "Unknown Opcode 0x05", 0x05 ),
	SYSTEM_LOADING( "SYSTEM_LOADING  ", "System Loading", 0x09 ),
	CHANNEL_CWID_UPDATE( "CHAN_CWID_UPDATE", "Channel FCC Callsign Update", 0x0B ),
	UNKNOWN( "UNKNOWN         ", "Unknown", -1 );

	private String mLabel;
	private String mDescription;
	private int mCode;
	
	private MotorolaOpcode( String label, String description, int code )
	{
		mLabel = label;
		mDescription = description;
		mCode = code;
	}
	
	public String getLabel()
	{
		return mLabel;
	}
	
	public String getDescription()
	{
		return mDescription;
	}
	
	public int getCode()
	{
		return mCode;
	}
	
	public static MotorolaOpcode fromValue( int value )
	{
		switch( value )
		{
			case 0x05:
				return UNKNOWN_OPCODE_5;
			case 0x09:
				return SYSTEM_LOADING;
			case 0x0B:
				return CHANNEL_CWID_UPDATE;
		}
		
		return UNKNOWN;
	}
}
