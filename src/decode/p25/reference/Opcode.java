package decode.p25.reference;

public enum Opcode
{
	GROUP_VOICE_CHANNEL_GRANT( "GRP_VCH_GRANT   ", "Group Voice Channel Grant", 0 ),
	RESERVED_01( "RESERVED_01     ", "Reserved", 1 ),
	GROUP_VOICE_CHANNEL_GRANT_UPDATE( "GRP_VCH_GRNT_UPD", "Group Voice Channel Grant Update", 2 ),
	GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT( "GRP_VCH_GRNT_UPX", "Group Voice Channel Grant Update - Explicit", 3 ),
	UNIT_TO_UNIT_VOICE_CHANNEL_GRANT( "UU_VCH_GRANT    ", "Unit-to-Unit Voice Channel Grant", 4 ),
	UNIT_TO_UNIT_ANSWER_REQUEST( "UU_ANS_REQ      ", "Unit-to-Unit Answer Request", 5 ),
	UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE( "UU_VCH_GRANT_UPD", "Unit-to-Unit Voice Channel Grant Update", 6 ),
	RESERVED_07( "RESERVED_07     ", "Reserved", 7 ),
	TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT( "TEL_INT_VCH_GRNT", "Telephone Interconnect Voice Channel Grant", 8 ),
	TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE( "TEL_INT_VCH_GRNU","Telephone Interconnect Voice Channel Grant Update", 9 ),
	TELEPHONE_INTERCONNECT_ANSWER_REQUEST( "TEL_INT_ANS_RQST", "Telephone Interconnect Answer Request", 10 ),
	RESERVED_0B( "RESERVED_0B     ", "Reserved", 11 ),
	RESERVED_0C( "RESERVED_0C     ", "Reserved", 12 ),
	RESERVED_0D( "RESERVED_0D     ", "Reserved", 13 ),
	RESERVED_0E( "RESERVED_0E     ", "Reserved", 14 ),
	RESERVED_0F( "RESERVED_0F     ", "Reserved", 15 ),
	INDIVIDUAL_DATA_CHANNEL_GRANT( "IND_DCH_GRANT   ", "Individual Data Channel Grant", 16 ),
	GROUP_DATA_CHANNEL_GRANT( "GRP_DCH_GRANT   ", "Group Data Channel Grant", 17 ),
	GROUP_DATA_CHANNEL_ANNOUNCEMENT( "GRP_DCH_ANNOUNCE", "Group Data Channel Announcement", 18 ),
	GROUP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT( "GRP_DCH_ANNC_EXP", "Group Data Channel Announcement-Explicit", 19 ),
	SNDCP_DATA_CHANNEL_GRANT( "SNDCP_DCH_GRANT ", "SNDCP Data Channel Grant", 20 ),
	SNDCP_DATA_PAGE_REQUEST( "SNDCP_DCH_PAG_RQ", "SNDCP Data Page Request", 21 ),
	SNDCP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT( "SNDCP_DCH_ANN_EX", "SNDCP Data Channel Announcement Explicit", 22 ),
	RESERVED_17( "RESERVED_17     ", "Reserved", 23 ),
	STATUS_UPDATE( "STATUS_UPDATE   ", "Status Update", 24 ),
	RESERVED_19( "RESERVED_19     ", "Reserved", 25 ),
	STATUS_QUERY( "STATUS_QUERY    ", "Status Query", 26 ),
	RESERVED_1B( "RESERVED_1B     ", "Reserved", 27 ),
	MESSAGE_UPDATE( "MESSAGE_UPDATE  ", "Message Update", 28 ),
	RADIO_UNIT_MONITOR_COMMAND( "RADIO_MONITR_CMD", "Radio Unit Monitor Command", 29 ),
	RESERVED_1E( "RESERVED_1E     ", "Reserved", 30 ),
	CALL_ALERT( "CALL_ALERT      ", "Call Alert", 31 ),
	ACKNOWLEDGE_RESPONSE_FNE( "ACK_RESPONSE_FNE", "Acknowledge Response - FNE", 32 ),
	QUEUED_RESPONSE( "QUEUED_RESPONSE ", "Queued Response", 33 ),
	RESERVED_22( "RESERVED_22     ", "Reserved", 34 ),
	RESERVED_23( "RESERVED_23     ", "Reserved", 35 ),
	EXTENDED_FUNCTION_COMMAND( "EXTNDED_FUNC_CMD", "Extended Function Command", 36 ),
	RESERVED_25( "RESERVED_25     ", "Reserved", 37 ),
	RESERVED_26( "RESERVED_26     ", "Reserved", 38 ),
	DENY_RESPONSE( "DENY_RESPONSE   ", "Deny Response", 39 ),
	GROUP_AFFILIATION_RESPONSE( "GRP_AFFIL_RESP  ", "Group Affiliation Response", 40 ),
	SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT( "SCCB_CCH_BCST_EX", "Secondary Control Channel Broadcast-Explicit", 41 ),
	GROUP_AFFILIATION_QUERY( "GRP_AFFIL_QUERY ", "Group Affiliation Query", 42 ),
	LOCATION_REGISTRATION_RESPONSE( "LOCN_REG_RESPONS", "Location Registration Response", 43 ),
	UNIT_REGISTRATION_RESPONSE( "UNIT_REG_RESPONS", "Unit Registration Response", 44 ),
	UNIT_REGISTRATION_COMMAND( "UNIT_REG_COMMAND", "Unit Registration Command", 45 ),
	AUTHENTICATION_COMMAND( "AUTH_COMMAND    ", "Authentication Command", 46 ),
	UNIT_DEREGISTRATION_ACKNOWLEDGE( "DE_REGIST_ACK   ", "De-Registration Acknowledge", 47 ),
	RESERVED_30( "RESERVED_30     ", "Reserved", 48 ),
	RESERVED_31( "RESERVED_31     ", "Reserved", 49 ),
	RESERVED_32( "RESERVED_32     ", "Reserved", 50 ),
	RESERVED_33( "RESERVED_33     ", "Reserved", 51 ),
	IDENTIFIER_UPDATE_VHF_UHF_BANDS( "IDEN_UPDATE_VUHF", "Identifier Update for VHF/UHF Bands", 52 ),
	TIME_DATE_ANNOUNCEMENT( "TIME_DATE_ANNOUN", "Time and Date Announcement", 53 ),
	ROAMING_ADDRESS_COMMAND( "ROAM_ADDR_CMD   ", "Roaming Address Command", 54 ),
	ROAMING_ADDRESS_UPDATE( "ROAM_ADDR_UPDATE", "Roaming Address Update", 55 ),
	SYSTEM_SERVICE_BROADCAST( "SYS_SVC_BCAST   ", "System Service Broadcast", 56 ),
	SECONDARY_CONTROL_CHANNEL_BROADCAST( "SEC_CCH_BROADCST", "Secondary Control Channel Broadcast", 57 ),
	RFSS_STATUS_BROADCAST( "RFSS_STATUS_BCST", "RFSS Status Broadcast", 58 ),
	NETWORK_STATUS_BROADCAST( "NET_STATUS_BCAST", "Network Status Broadcast", 59 ),
	ADJACENT_STATUS_BROADCAST( "NGHBR_STAT_BCAST", "Adjacent Status Broadcast", 60 ),
	IDENTIFIER_UPDATE_NON_VUHF( "IDEN_UPDATE     ", "Identifier Update", 61 ),
	PROTECTION_PARAMETER_BROADCAST( "ENCRYPT_PAR_BCST", "Protection Parameter Broadcast", 62 ),
	PROTECTION_PARAMETER_UPDATE( "ENCRYPT_PAR_UPDT", "Protection Parameter Update", 63 ),
	UNKNOWN( "UNKNOWN         ", "Unknown", -1 );

	private String mLabel;
	private String mDescription;
	private int mCode;
	
	private Opcode( String label, String description, int code )
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
	
	public static Opcode fromValue( int value )
	{
		if( 0 <= value && value <= 63 )
		{
			return values()[ value ];
		}
		
		return UNKNOWN;
	}
}
