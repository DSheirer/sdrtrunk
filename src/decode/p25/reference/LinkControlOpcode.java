package decode.p25.reference;

public enum LinkControlOpcode
{
	GROUP_VOICE_CHANNEL_USER( "GRP_VCH_USER    ", "Group Voice Channel User", 0 ),
	RESERVED_01( "RESERVED_01     ", "Reserved", 1 ),
	GROUP_VOICE_CHANNEL_UPDATE( "GRP_VCH_UPDATE  ", "Group Voice Channel Grant Update", 2 ),
	UNIT_TO_UNIT_VOICE_CHANNEL_USER( "UU_VCH_USER     ", "Unit-to-Unit Voice Channel User", 3 ),
	GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT( "GRP_VCH_UPD_EXPL", "Group Voice Channel Update Explicit", 4 ),
	UNIT_TO_UNIT_ANSWER_REQUEST( "UU_ANS_REQ      ", "Unit-to-Unit Answer Request", 5 ),
	TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER( "TEL_INT_VCH_USER", "Telephone Interconnect Voice Channel User", 6 ),
	TELEPHONE_INTERCONNECT_ANSWER_REQUEST( "TEL_INT_ANS_RQST", "Telephone Interconnect Answer Request", 7 ),
	RESERVED_08( "RESERVED_08     ", "Reserved", 8 ),
	RESERVED_09( "RESERVED_09     ", "Reserved", 9 ),
	RESERVED_0A( "RESERVED_0A     ", "Reserved", 10 ),
	RESERVED_0B( "RESERVED_0B     ", "Reserved", 11 ),
	RESERVED_0C( "RESERVED_0C     ", "Reserved", 12 ),
	RESERVED_0D( "RESERVED_0D     ", "Reserved", 13 ),
	RESERVED_0E( "RESERVED_0E     ", "Reserved", 14 ),
	CALL_TERMINATION_OR_CANCELLATION( "CALL TERMINATION", "Call Termination Cancellation", 15 ),
	GROUP_AFFILIATION_QUERY( "GRP_AFFIL_QUERY ", "Group Affiliation Query", 16 ),
	UNIT_REGISTRATION_COMMAND( "UNIT_REG_COMMAND", "Unit Registration Command", 17 ),
	UNIT_AUTHENTICATION_COMMAND( "UNIT_AUTHEN_CMD ", "Unit Authentication Command", 18 ),
	STATUS_QUERY( "STATUS QUERY    ", "Status Query", 19 ),
	STATUS_UPDATE( "STATUS_UPDATE   ", "Status Update", 20 ),
	MESSAGE_UPDATE( "MESSAGE UPDATE  ", "Message Update", 21 ),
	CALL_ALERT( "CALL ALERT", "Call Alert", 22 ),
	EXTENDED_FUNCTION_COMMAND( "EXT FUNC COMMAND", "Extended Function Command", 23 ),
	CHANNEL_IDENTIFIER_UPDATE( "CHAN IDEN UPDATE", "Channel Identifier Update", 24 ),
	CHANNEL_IDENTIFIER_UPDATE_EXPLICIT( "CHAN IDEN UPD EX", "Channel Identifier Update Explicit", 25 ),
	RESERVED_1A( "RESERVED_1A     ", "Reserved", 26 ),
	RESERVED_1B( "RESERVED_1B     ", "Reserved", 27 ),
	RESERVED_1C( "RESERVED_1C     ", "Reserved", 28 ),
	RESERVED_1D( "RESERVED_1D     ", "Reserved", 29 ),
	RESERVED_1E( "RESERVED_1E     ", "Reserved", 30 ),
	RESERVED_1F( "RESERVED_1F     ", "Reserved", 31 ),
	SYSTEM_SERVICE_BROADCAST( "SYS_SVC_BCAST   ", "System Service Broadcast", 32 ),
	SECONDARY_CONTROL_CHANNEL_BROADCAST( "SEC_CCH_BROADCST", "Secondary Control Channel Broadcast", 33 ),
	ADJACENT_SITE_STATUS_BROADCAST( "ADJ SITE STATUS ", "Adjacent Site Status Broadcast", 34 ),
	RFSS_STATUS_BROADCAST( "RFSS_STATUS_BCST", "RFSS Status Broadcast", 35 ),
	NETWORK_STATUS_BROADCAST( "NET_STATUS_BCAST", "Network Status Broadcast", 36 ),
	PROTECTION_PARAMETER_BROADCAST( "ENCRYPT_PAR_BCST", "Protection Parameter Broadcast", 37 ),
	SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT( "SCCB_CCH_BCST_EX", "Secondary Control Channel Broadcast-Explicit", 38 ),
	ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT( "ADJ SITE STAT EX", "Adjacent Site Status Broadcast Explicit", 39 ),
	RFSS_STATUS_BROADCAST_EXPLICIT( "RFSS STAT BCST E", "RFSS Status Broadcast Explicit", 40 ),
	NETWORK_STATUS_BROADCAST_EXPLICIT( "NET STAT BCAST E", "Network Status Broadcast", 41 ),
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
	
	private LinkControlOpcode( String label, String description, int code )
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
	
	public static LinkControlOpcode fromValue( int value )
	{
		if( 0 <= value && value <= 63 )
		{
			return values()[ value ];
		}
		
		return UNKNOWN;
	}
}
