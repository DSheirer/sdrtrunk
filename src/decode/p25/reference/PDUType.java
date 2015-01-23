package decode.p25.reference;

public enum PDUType
{
	/* Outbound */
	SN_ACTIVATE_TDS_CONTEXT_ACCEPT( "SN-ACTIVATE TDS CONTEXT ACCEPT", 0 ),
	SN_DEACTIVATE_TDS_CONTEXT_ACCEPT( "SN-DEACTIVATE TDS CONTEXT ACCEPT", 1 ),
	SN_DEACTIVATE_TDS_CONTEXT_REQUEST( "SN-DEACTIVATE TDS CONTEXT REQUEST", 2 ),
	SN_ACTIVATE_TDS_CONTEXT_REJECT( "SN-ACTIVATE TDS CONTEXT REJECT", 3 ),
	SN_RF_UNCONFIRMED_DATA( "SN-RF UNCONFIRMED DATA", 4 ),
	SN_RF_CONFIRMED_DATA( "SN-RF CONFIRMED DATA", 5 ),
	
	/* Inbound */
	SN_ACTIVATE_TDS_CONTEXT_REQUEST( "SN-ACTIVATE TDS CONTEXT REQUEST", 0 ),

	UNKNOWN( "UNKNOWN", -1 );
	
	private String mLabel;
	private int mValue;
	
	private PDUType( String label, int value )
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
	
	public static PDUType fromValue( int value, boolean outbound )
	{
		if( outbound && 0 <= value && value <= 5 )
		{
			return values()[ value ];
		}
		else if( value == 0 && !outbound )
		{
			return SN_ACTIVATE_TDS_CONTEXT_REQUEST;
		}
		
		return UNKNOWN;
	}
}
