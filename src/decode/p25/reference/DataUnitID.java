package decode.p25.reference;

/**
 * The Data Unit ID (DUID) is part of the Network ID (NID) field and indicates
 * the type of message.
 */
public enum DataUnitID
{
	NID  ( -1,   64, "Network and Data Unit ID", false ),
	HDU  (  0,  722, "Header Data Unit", false ),
	TDU  (  3,   64, "Simple Terminator Data Unit", false ),
	LDU1 (  5, 1630, "Logical Link Data Unit 1", true ),
	TSBK1(  7,  260, "Trunking Signaling Block", false ), //Single Block
	TSBK2(  7,  260, "Trunking Signaling Block", false ), //Single Block
	TSBK3(  7,  260, "Trunking Signaling Block", false ), //Single Block
	LDU2 ( 10, 1632, "Logical Link Data Unit 2", true ),
	PDU1 ( 12,  456, "Packet Data Unit", false ),
	PDU2 ( 12,  652, "Packet Data Unit", false ),
	PDU3 ( 12,  848, "Packet Data Unit", false ),
	TDULC( 15,  372, "Terminator Data Unit With Link Control", false ),
	UNKN ( -1,    0, "Unknown", false );
	
	private int mValue;
	private int mMessageLength;
	private String mLabel;
	private boolean mParity;
	
	private DataUnitID( int value, int length, String label, boolean parity )
	{
		mValue = value;
		mMessageLength = length;
		mLabel = label;
		mParity = parity;
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
				return PDU1;
			case 15:
				return TDULC;
			default:
				return UNKN;
		}
	}
}
