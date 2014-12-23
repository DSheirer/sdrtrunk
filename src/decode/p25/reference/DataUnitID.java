package decode.p25.reference;

/**
 * The Data Unit ID (DUID) is part of the Network ID (NID) field and indicates
 * the type of message.
 */
public enum DataUnitID
{
	NID  ( -1,   64, false, "NID  ", "Network and Data Unit ID" ),
	HDU  (  0,  722, false, "HDU  ", "Header Data Unit" ),
	TDU  (  3,   64, false, "TDU  ", "Simple Terminator Data Unit" ),
	LDU1 (  5, 1630, true,  "LDU1 ", "Logical Link Data Unit 1" ),
	TSBK1(  7,  260, false, "TSBK1", "Trunking Signaling Block" ),
	TSBK2(  7,  260, false, "TSBK2", "Trunking Signaling Block" ),
	TSBK3(  7,  260, false, "TSBK3", "Trunking Signaling Block" ),
	LDU2 ( 10, 1632, true,  "LDU2 ", "Logical Link Data Unit 2" ),
	PDU0 ( 12,  260, false, "PDU0 ", "Packet Header Data Unit" ),
	PDU1 ( 12,  356, false, "PDU1 ", "Packet Data Unit" ),
	PDU2 ( 12,  452, false, "PDU2 ", "Packet Data Unit" ),
	PDU3 ( 12,  548, false, "PDU3 ", "Packet Data Unit" ),
	TDULC( 15,  352, false, "TDULC", "Terminator Data Unit With Link Control" ),
	UNKN ( -1,    0, false, "UNKWN", "Unknown" );
	
	private int mValue;
	private int mMessageLength;
	private boolean mParity;
	private String mLabel;
	private String mDescription;
	
	private DataUnitID( int value, int length, boolean parity, String label, 
			String description )
	{
		mValue = value;
		mMessageLength = length;
		mParity = parity;
		mLabel = label;
		mDescription = description;
	}
	
	public int getValue()
	{
		return mValue;
	}
	
	public int getMessageLength()
	{
		return mMessageLength;
	}
	
	public String getLabel()
	{
		return mLabel;
	}
	
	public String getDescription()
	{
		return mDescription;
	}
	
	public boolean getParity()
	{
		return mParity;
	}
	
	public static DataUnitID fromValue( int value )
	{
		switch( value )
		{
			case 0:
				return HDU;
			case 3:
				return TDU;
			case 5:
				return LDU1;
			case 7:
				return TSBK1;
			case 10:
				return LDU2;
			case 12:
				return PDU0;
			case 15:
				return TDULC;
			default:
				return UNKN;
		}
	}
}
