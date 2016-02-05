package module.decode.p25.reference;

public enum PDUType
{
	/* Outbound */
	SNDCP_ACTIVATE_TDS_CONTEXT_ACCEPT( "SNDCP-ACTIVATE TDS CONTEXT ACCEPT", 0 ),
	SNDCP_DEACTIVATE_TDS_CONTEXT_ACCEPT( "SNDCP-DEACTIVATE TDS CONTEXT ACCEPT", 1 ),
	SNDCP_DEACTIVATE_TDS_CONTEXT_REQUEST( "SNDCP-DEACTIVATE TDS CONTEXT REQUEST", 2 ),
	SNDCP_ACTIVATE_TDS_CONTEXT_REJECT( "SNDCP-ACTIVATE TDS CONTEXT REJECT", 3 ),
	SNDCP_RF_UNCONFIRMED_DATA( "SNDCP-RF UNCONFIRMED DATA", 4 ),
	SNDCP_RF_CONFIRMED_DATA( "SNDCP-RF CONFIRMED DATA", 5 ),
	PDU_TYPE_6( "PDU TYPE 6 UNKNOWN", 6 ),
	PDU_TYPE_7( "PDU TYPE 7 UNKNOWN", 7 ),
	PDU_TYPE_8( "PDU TYPE 8 UNKNOWN", 8 ),
	PDU_TYPE_9( "PDU TYPE 9 UNKNOWN", 9 ),
	PDU_TYPE_10( "PDU TYPE 10 UNKNOWN", 10 ),
	PDU_TYPE_11( "PDU TYPE 11 UNKNOWN", 11 ),
	
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
