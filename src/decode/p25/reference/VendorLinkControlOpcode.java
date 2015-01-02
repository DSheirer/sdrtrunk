package decode.p25.reference;

public enum VendorLinkControlOpcode
{
	RESERVED_00( "RESERVED_00     ", "Reserved", 0 ),
	RESERVED_01( "RESERVED_01     ", "Reserved", 1 ),
	RESERVED_02( "RESERVED_02     ", "Reserved", 2 ),
	RESERVED_03( "RESERVED_03     ", "Reserved", 3 ),
	RESERVED_04( "RESERVED_04     ", "Reserved", 4 ),
	RESERVED_05( "RESERVED_05     ", "Reserved", 5 ),
	RESERVED_06( "RESERVED_06     ", "Reserved", 6 ),
	RESERVED_07( "RESERVED_07     ", "Reserved", 7 ),
	RESERVED_08( "RESERVED_0B     ", "Reserved", 8 ),
	RESERVED_09( "RESERVED_0B     ", "Reserved", 9 ),
	RESERVED_0A( "RESERVED_0B     ", "Reserved", 10 ),
	RESERVED_0B( "RESERVED_0B     ", "Reserved", 11 ),
	RESERVED_0C( "RESERVED_0C     ", "Reserved", 12 ),
	RESERVED_0D( "RESERVED_0D     ", "Reserved", 13 ),
	RESERVED_0E( "RESERVED_0E     ", "Reserved", 14 ),
	RESERVED_0F( "RESERVED_0F     ", "Reserved", 15 ),
	RESERVED_10( "RESERVED_10     ", "Reserved", 16 ),
	RESERVED_11( "RESERVED_11     ", "Reserved", 17 ),
	RESERVED_12( "RESERVED_12     ", "Reserved", 18 ),
	RESERVED_13( "RESERVED_13     ", "Reserved", 19 ),
	RESERVED_14( "RESERVED_14     ", "Reserved", 20 ),
	RESERVED_15( "RESERVED_15     ", "Reserved", 21 ),
	RESERVED_16( "RESERVED_16     ", "Reserved", 22 ),
	RESERVED_17( "RESERVED_17     ", "Reserved", 23 ),
	RESERVED_18( "RESERVED_18     ", "Reserved", 24 ),
	RESERVED_19( "RESERVED_19     ", "Reserved", 25 ),
	RESERVED_1A( "RESERVED_1A     ", "Reserved", 26 ),
	RESERVED_1B( "RESERVED_1B     ", "Reserved", 27 ),
	RESERVED_1C( "RESERVED_1C     ", "Reserved", 28 ),
	RESERVED_1D( "RESERVED_1D     ", "Reserved", 29 ),
	RESERVED_1E( "RESERVED_1E     ", "Reserved", 30 ),
	RESERVED_1F( "RESERVED_1F     ", "Reserved", 31 ),
	RESERVED_20( "RESERVED_20     ", "Reserved", 32 ),
	RESERVED_21( "RESERVED_21     ", "Reserved", 33 ),
	RESERVED_22( "RESERVED_22     ", "Reserved", 34 ),
	RESERVED_23( "RESERVED_23     ", "Reserved", 35 ),
	RESERVED_24( "RESERVED_24     ", "Reserved", 36 ),
	RESERVED_25( "RESERVED_25     ", "Reserved", 37 ),
	RESERVED_26( "RESERVED_26     ", "Reserved", 38 ),
	RESERVED_27( "RESERVED_27     ", "Reserved", 39 ),
	RESERVED_28( "RESERVED_28     ", "Reserved", 40 ),
	RESERVED_29( "RESERVED_29     ", "Reserved", 41 ),
	RESERVED_2A( "RESERVED_2A     ", "Reserved", 42 ),
	RESERVED_2B( "RESERVED_2B     ", "Reserved", 43 ),
	RESERVED_2C( "RESERVED_2C     ", "Reserved", 44 ),
	RESERVED_2D( "RESERVED_2D     ", "Reserved", 45 ),
	RESERVED_2E( "RESERVED_2E     ", "Reserved", 46 ),
	RESERVED_2F( "RESERVED_2F     ", "Reserved", 47 ),
	RESERVED_30( "RESERVED_30     ", "Reserved", 48 ),
	RESERVED_31( "RESERVED_31     ", "Reserved", 49 ),
	RESERVED_32( "RESERVED_32     ", "Reserved", 50 ),
	RESERVED_33( "RESERVED_33     ", "Reserved", 51 ),
	RESERVED_34( "RESERVED_34     ", "Reserved", 52 ),
	RESERVED_35( "RESERVED_35     ", "Reserved", 53 ),
	RESERVED_36( "RESERVED_36     ", "Reserved", 54 ),
	RESERVED_37( "RESERVED_37     ", "Reserved", 55 ),
	RESERVED_38( "RESERVED_38     ", "Reserved", 56 ),
	RESERVED_39( "RESERVED_39     ", "Reserved", 57 ),
	RESERVED_3A( "RESERVED_3A     ", "Reserved", 58 ),
	RESERVED_3B( "RESERVED_3B     ", "Reserved", 59 ),
	RESERVED_3C( "RESERVED_3C     ", "Reserved", 60 ),
	RESERVED_3D( "RESERVED_3D     ", "Reserved", 61 ),
	RESERVED_3E( "RESERVED_3E     ", "Reserved", 62 ),
	RESERVED_3F( "RESERVED_3F     ", "Reserved", 63 ),
	UNKNOWN( "UNKNOWN         ", "Unknown", -1 );

	private String mLabel;
	private String mDescription;
	private int mCode;
	
	private VendorLinkControlOpcode( String label, String description, int code )
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
	
	public static VendorLinkControlOpcode fromValue( int value )
	{
		if( 0 <= value && value <= 63 )
		{
			return values()[ value ];
		}
		
		return UNKNOWN;
	}
}
