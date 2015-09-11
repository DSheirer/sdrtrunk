package audio.metadata;

public enum MetadataType
{
	CHANNEL_NUMBER( "Channel Number" ),
	CHANNEL_NAME( "Channel Name" ),
	DETAILS( "Details" ),
	ESN( "ESN" ),
	FLEETSYNC( "Fleetsync" ),
	FREQUENCY( "Frequency" ),
	FROM( "From" ),
	MDC1200( "MDC-1200" ),
	MOBILE_ID( "Mobile ID" ),
	MPT1327( "MPT-1327" ),
	PROTOCOL( "Protocol" ),
	SITE_ID( "Site ID" ),
	STATUS( "Status" ),
	SYSTEM( "System" ),
	TO( "To" ),
	UNIQUE_ID( "Unique ID" ),
	
	TEMPORAL_RESET( "Reset" );
	
	private String mLabel;
	
	private MetadataType( String label )
	{
		mLabel = label;
	}
	
	public String getLabel()
	{
		return mLabel;
	}
	
	public String toString()
	{
		return mLabel;
	}
}
