package decode.p25.reference;

public enum Opcode
{
	GROUP_VOICE_CHANNEL_GRANT( "Group Voice Channel Grant", 0 ),
	RESERVED_01( "Reserved", 1 ),
	GROUP_VOICE_CHANNEL_GRANT_UPDATE( "Group Voice Channel Grant Update", 2 ),
	GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT( "Group Voice Channel Grant Update - Explicit", 3 ),
	UNIT_TO_UNIT_VOICE_CHANNEL_GRANT( "Unit-to-Unit Voice Channel Grant", 4 ),
	UNIT_TO_UNIT_ANSWER_REQUEST( "Unit-to-Unit Answer Request", 5 ),
	UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE( "Unit-to-Unit Voice Channel Grant Update", 6 ),
	RESERVED_07( "Reserved", 7 ),
	TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT( "Telephone Interconnect Voice Channel Grant", 8 ),
	TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE( "Telephone Interconnect Voice Channel Grant Update", 9 ),
	TELEPHONE_INTERCONNECT_ANSWER_REQUEST( "Telephone Interconnect Answer Request", 10 ),
	RESERVED_0B( "Reserved", 11 ),
	RESERVED_0C( "Reserved", 12 ),
	RESERVED_0D( "Reserved", 13 ),
	RESERVED_0E( "Reserved", 14 ),
	RESERVED_0F( "Reserved", 15 ),
	INDIVIDUAL_DATA_CHANNEL_GRANT( "Individual Data Channel Grant", 16 ),
	GROUP_DATA_CHANNEL_GRANT( "Group Data Channel Grant", 17 ),
	GROUP_DATA_CHANNEL_ANNOUNCEMENT( "Group Data Channel Announcement", 18 ),
	GROUP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT( "Group Data Channel Announcement-Explicit", 19 ),
	SNDCP_DATA_CHANNEL_GRANT( "SNDCP Data Channel Grant", 20 ),
	SNDCP_DATA_PAGE_REQUEST( "SNDCP Data Page Request", 21 ),
	SNDCP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT( "SNDCP Data Channel Announcement Explicit", 22 ),
	RESERVED_17( "Reserved", 23 ),
	STATUS_UPDATE( "Status Update", 24 ),
	RESERVED_19( "Reserved", 25 ),
	STATUS_QUERY( "Status Query", 26 ),
	RESERVED_1B( "Reserved", 27 ),
	MESSAGE_UPDATE( "Message Update", 28 ),
	RADIO_UNIT_MONITOR_COMMAND( "Radio Unit Monitor Command", 29 ),
	RESERVED_1E( "Reserved", 30 ),
	CALL_ALERT( "Call Alert", 31 ),
	ACKNOWLEDGE_RESPONSE_FNE( "Acknowledge Response - FNE", 32 ),
	QUEUED_RESPONSE( "Queued Response", 33 ),
	RESERVED_22( "Reserved", 34 ),
	RESERVED_23( "Reserved", 35 ),
	EXTENDED_FUNCTION_COMMAND( "Extended Function Command", 36 ),
	RESERVED_25( "Reserved", 37 ),
	RESERVED_26( "Reserved", 38 ),
	DENY_RESPONSE( "Deny Response", 39 ),
	GROUP_AFFILIATION_RESPONSE( "Group Affiliation Response", 40 ),
	SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT( "Secondary Control Channel Broadcast-Explicit", 41 ),
	GROUP_AFFILIATION_QUERY( "Group Affiliation Query", 42 ),
	LOCATION_REGISTRATION_RESPONSE( "Location Registration Response", 43 ),
	UNIT_REGISTRATION_RESPONSE( "Unit Registration Response", 44 ),
	UNIT_REGISTRATION_COMMAND( "Unit Registration Command", 45 ),
	AUTHENTICATION_COMMAND( "Authentication Command", 46 ),
	UNIT_DEREGISTRATION_ACKNOWLEDGE( "De-Registration Acknowledge", 47 ),
	RESERVED_30( "Reserved", 48 ),
	RESERVED_31( "Reserved", 49 ),
	RESERVED_32( "Reserved", 50 ),
	RESERVED_33( "Reserved", 51 ),
	IDENTIFIER_UPDATE_VHF_UHF_BANDS( "Identifier Update for VHF/UHF Bands", 52 ),
	TIME_DATE_ANNOUNCEMENT( "Time and Date Announcement", 53 ),
	ROAMING_ADDRESS_COMMAND( "Roaming Address Command", 54 ),
	ROAMING_ADDRESS_UPDATE( "Roaming Address Update", 55 ),
	SYSTEM_SERVICE_BROADCAST( "System Service Broadcast", 56 ),
	SECONDARY_CONTROL_CHANNEL_BROADCAST( "Secondary Control Channel Broadcast", 57 ),
	RFSS_STATUS_BROADCAST( "RFSS Status Broadcast", 58 ),
	NETWORK_STATUS_BROADCAST( "Network Status Broadcast", 59 ),
	ADJACENT_STATUS_BROADCAST( "Adjacent Status Broadcast", 60 ),
	IDENTIFIER_UPDATE( "Identifier Update", 61 ),
	PROTECTION_PARAMETER_BROADCAST( "Protection Parameter Broadcast", 62 ),
	PROTECTION_PARAMETER_UPDATE( "Protection Parameter Update", 63 ),
	UNKNOWN( "Unknown", -1 );
	
	private String mDescription;
	private int mCode;
	
	private Opcode( String description, int code )
	{
		mDescription = description;
		mCode = code;
	}
	
	public String getDescription()
	{
		return mDescription;
	}
	
	public int getCode()
	{
		return mCode;
	}
	
	public static Opcode fromValue( int value )
	{
		if( 0 <= value && value <= 63 )
		{
			return values()[ value ];
		}
		
		return UNKNOWN;
	}
}
