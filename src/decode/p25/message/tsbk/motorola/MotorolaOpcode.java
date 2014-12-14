package decode.p25.message.tsbk.motorola;

public enum MotorolaOpcode
{
	OP00( "OP00:UNKNOWN    ", "Opcode 0x00 Unknown", 0x00 ),
	OP01( "OP01:UNKNOWN    ", "Opcode 0x01 Unknown", 0x01 ),
	OP02( "OP02:UNKNOWN    ", "Opcode 0x02 Unknown", 0x02 ),
	OP03( "OP03:UNKNOWN    ", "Opcode 0x03 Unknown", 0x03 ),
	OP04( "OP04:UNKNOWN    ", "Opcode 0x04 Unknown", 0x04 ),
	OP05( "OP05:UNKNOWN    ", "Opcode 0x05 Unknown", 0x05 ),
	OP06( "OP06:UNKNOWN    ", "Opcode 0x06 Unknown", 0x06 ),
	OP07( "OP07:UNKNOWN    ", "Opcode 0x07 Unknown", 0x07 ),
	OP08( "OP08:UNKNOWN    ", "Opcode 0x08 Unknown", 0x08 ),
	OP09( "OP09:SYSTEM_LOAD", "Opcode 0x09 System Loading", 0x09 ),
	OP0A( "OP0A:UNKNOWN    ", "Opcode 0x0A Unknown", 0x0A ),
	OP0B( "OP0B:CWID_UPDATE", "Opcode 0x0B FCC Callsign", 0x0B ),
	OP0C( "OP0C:UNKNOWN    ", "Opcode 0x0C Unknown", 0x0C ),
	OP0D( "OP0D:UNKNOWN    ", "Opcode 0x0D Unknown", 0x0D ),
	OP0E( "OP0E:UNKNOWN    ", "Opcode 0x0E Unknown", 0x0E ),
	OP0F( "OP0F:UNKNOWN    ", "Opcode 0x0F Unknown", 0x0F ),
	OP10( "OP10:UNKNOWN    ", "Opcode 0x10 Unknown", 0x10 ),
	OP11( "OP11:UNKNOWN    ", "Opcode 0x11 Unknown", 0x11 ),
	OP12( "OP12:UNKNOWN    ", "Opcode 0x12 Unknown", 0x12 ),
	OP13( "OP13:UNKNOWN    ", "Opcode 0x13 Unknown", 0x13 ),
	OP14( "OP14:UNKNOWN    ", "Opcode 0x14 Unknown", 0x14 ),
	OP15( "OP15:UNKNOWN    ", "Opcode 0x15 Unknown", 0x15 ),
	OP16( "OP16:UNKNOWN    ", "Opcode 0x16 Unknown", 0x16 ),
	OP17( "OP17:UNKNOWN    ", "Opcode 0x17 Unknown", 0x17 ),
	OP18( "OP18:UNKNOWN    ", "Opcode 0x18 Unknown", 0x18 ),
	OP19( "OP19:UNKNOWN    ", "Opcode 0x19 Unknown", 0x19 ),
	OP1A( "OP1A:UNKNOWN    ", "Opcode 0x1A Unknown", 0x1A ),
	OP1B( "OP1B:UNKNOWN    ", "Opcode 0x1B Unknown", 0x1B ),
	OP1C( "OP1C:UNKNOWN    ", "Opcode 0x1C Unknown", 0x1C ),
	OP1D( "OP1D:UNKNOWN    ", "Opcode 0x1D Unknown", 0x1D ),
	OP1E( "OP1E:UNKNOWN    ", "Opcode 0x1E Unknown", 0x1E ),
	OP1F( "OP1F:UNKNOWN    ", "Opcode 0x1F Unknown", 0x1F ),
	OP20( "OP20:UNKNOWN    ", "Opcode 0x20 Unknown", 0x20 ),
	OP21( "OP21:UNKNOWN    ", "Opcode 0x21 Unknown", 0x21 ),
	OP22( "OP22:UNKNOWN    ", "Opcode 0x22 Unknown", 0x22 ),
	OP23( "OP23:UNKNOWN    ", "Opcode 0x23 Unknown", 0x23 ),
	OP24( "OP24:UNKNOWN    ", "Opcode 0x24 Unknown", 0x24 ),
	OP25( "OP25:UNKNOWN    ", "Opcode 0x25 Unknown", 0x25 ),
	OP26( "OP26:UNKNOWN    ", "Opcode 0x26 Unknown", 0x26 ),
	OP27( "OP27:UNKNOWN    ", "Opcode 0x27 Unknown", 0x27 ),
	OP28( "OP28:UNKNOWN    ", "Opcode 0x28 Unknown", 0x28 ),
	OP29( "OP29:UNKNOWN    ", "Opcode 0x29 Unknown", 0x29 ),
	OP2A( "OP2A:UNKNOWN    ", "Opcode 0x2A Unknown", 0x2A ),
	OP2B( "OP2B:UNKNOWN    ", "Opcode 0x2B Unknown", 0x2B ),
	OP2C( "OP2C:UNKNOWN    ", "Opcode 0x2C Unknown", 0x2C ),
	OP2D( "OP2D:UNKNOWN    ", "Opcode 0x2D Unknown", 0x2D ),
	OP2E( "OP2E:UNKNOWN    ", "Opcode 0x2E Unknown", 0x2E ),
	OP2F( "OP2F:UNKNOWN    ", "Opcode 0x2F Unknown", 0x2F ),
	OP30( "OP30:UNKNOWN    ", "Opcode 0x30 Unknown", 0x30 ),
	OP31( "OP31:UNKNOWN    ", "Opcode 0x31 Unknown", 0x31 ),
	OP32( "OP32:UNKNOWN    ", "Opcode 0x32 Unknown", 0x32 ),
	OP33( "OP33:UNKNOWN    ", "Opcode 0x33 Unknown", 0x33 ),
	OP34( "OP34:UNKNOWN    ", "Opcode 0x34 Unknown", 0x34 ),
	OP35( "OP35:UNKNOWN    ", "Opcode 0x35 Unknown", 0x35 ),
	OP36( "OP36:UNKNOWN    ", "Opcode 0x36 Unknown", 0x36 ),
	OP37( "OP37:UNKNOWN    ", "Opcode 0x37 Unknown", 0x37 ),
	OP38( "OP38:UNKNOWN    ", "Opcode 0x38 Unknown", 0x38 ),
	OP39( "OP39:UNKNOWN    ", "Opcode 0x39 Unknown", 0x39 ),
	OP3A( "OP3A:UNKNOWN    ", "Opcode 0x3A Unknown", 0x3A ),
	OP3B( "OP3B:UNKNOWN    ", "Opcode 0x3B Unknown", 0x3B ),
	OP3C( "OP3C:UNKNOWN    ", "Opcode 0x3C Unknown", 0x3C ),
	OP3D( "OP3D:UNKNOWN    ", "Opcode 0x3D Unknown", 0x3D ),
	OP3E( "OP3E:UNKNOWN    ", "Opcode 0x3E Unknown", 0x3E ),
	OP3F( "OP3F:UNKNOWN    ", "Opcode 0x3F Unknown", 0x3F ),
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
	
	public String toString()
	{
		return getLabel();
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
		if( 0 <= value && value <= 0x3F )
		{
			return MotorolaOpcode.values()[ value ];
		}
		
		return UNKNOWN;
	}
}
