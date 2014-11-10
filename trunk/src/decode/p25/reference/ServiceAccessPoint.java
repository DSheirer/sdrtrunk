package decode.p25.reference;

public enum ServiceAccessPoint
{
	SAP_0(  "0", 0 ),
	SAP_1(  "1", 1 ),
	SAP_2(  "2", 2 ),
	SAP_3(  "3", 3 ),
	SAP_4(  "4", 4 ),
	SAP_5(  "5", 5 ),
	SAP_6(  "6", 6 ),
	SAP_7(  "7", 7 ),
	SAP_8(  "8", 8 ),
	SAP_9(  "9", 9 ),
	SAP_10( "10", 10 ),
	SAP_11( "11", 11 ),
	SAP_12( "12", 12 ),
	SAP_13( "13", 13 ),
	SAP_14( "14", 14 ),
	SAP_15( "15", 15 ),
	SAP_16( "16", 16 ),
	SAP_17( "17", 17 ),
	SAP_18( "18", 18 ),
	SAP_19( "19", 19 ),
	SAP_20( "20", 20 ),
	SAP_21( "21", 21 ),
	SAP_22( "22", 22 ),
	SAP_23( "23", 23 ),
	SAP_24( "24", 24 ),
	SAP_25( "25", 25 ),
	SAP_26( "26", 26 ),
	SAP_27( "27", 27 ),
	SAP_28( "28", 28 ),
	SAP_29( "29", 29 ),
	SAP_30( "30", 30 ),
	SAP_31( "31", 31 ),
	SAP_32( "32", 32 ),
	SAP_33( "33", 33 ),
	SAP_34( "34", 34 ),
	SAP_35( "35", 35 ),
	SAP_36( "36", 36 ),
	SAP_37( "37", 37 ),
	SAP_38( "38", 38 ),
	SAP_39( "39", 39 ),
	SAP_40( "40", 40 ),
	SAP_41( "41", 41 ),
	SAP_42( "42", 42 ),
	SAP_43( "43", 43 ),
	SAP_44( "44", 44 ),
	SAP_45( "45", 45 ),
	SAP_46( "46", 46 ),
	SAP_47( "47", 47 ),
	SAP_48( "48", 48 ),
	SAP_49( "49", 49 ),
	SAP_50( "50", 50 ),
	SAP_51( "51", 51 ),
	SAP_52( "52", 52 ),
	SAP_53( "53", 53 ),
	SAP_54( "54", 54 ),
	SAP_55( "55", 55 ),
	SAP_56( "56", 56 ),
	SAP_57( "57", 57 ),
	SAP_58( "58", 58 ),
	SAP_59( "59", 59 ),
	SAP_60( "60", 60 ),
	UNENCRYPTED_TRUNKING_CONTROL( "OPEN TRUNK", 61 ),
	SAP_62( "62", 62 ),
	ENCRYPTED_TRUNKING_CONTROL( "ENC TRUNK", 63 ),
	UKNOWN( "UNKN", -1 );

	private String mLabel;
	private int mValue;
	
	private ServiceAccessPoint( String label, int value )
	{
		mLabel = label;
		mValue = value;
	}
	
	public String getLabel()
	{
		return mLabel;
	}
	
	public int getValue()
	{
		return mValue;
	}
	
	public static ServiceAccessPoint fromValue( int value )
	{
		if( 0 <= value && value <= 63 )
		{
			return values()[ value ];
		}
		
		return UKNOWN;
	}
}
