package module.decode.p25.message.tdu.lc;

import module.decode.p25.message.tsbk.osp.control.SystemService;
import module.decode.p25.reference.LinkControlOpcode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdjacentSiteStatusBroadcastExplicit extends TDULinkControlMessage
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( AdjacentSiteStatusBroadcastExplicit.class );

	public static final int[] LRA = { 72,73,74,75,88,89,90,91 };
	public static final int[] TRANSMIT_IDENTIFIER = { 92,93,94,95 };
	public static final int[] TRANSMIT_CHANNEL = { 96,97,98,99,112,113,114,115,
		116,117,118,119 };
	public static final int[] RFSS_ID = { 120,121,122,123,136,137,138,139 };
	public static final int[] SITE_ID = { 140,141,142,143,144,145,146,147 };
	
	public static final int[] RECEIVE_IDENTIFIER = { 160,161,162,163 };
	public static final int[] RECEIVE_CHANNEL = { 164,165,166,167,168,169,170,
		171,184,185,186,187 };
	
	public AdjacentSiteStatusBroadcastExplicit( TDULinkControlMessage source )
	{
		super( source );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT.getDescription();
    }

	@Override
	public String getMessage()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getMessageStub() );
		
		sb.append( " LRA:" + getLocationRegistrationArea() );
		
		sb.append( " SITE:" + getRFSubsystemID() + "-" + getSiteID() );
		
		sb.append( " DNLINK:" + getTransmitChannelNumber() );
		
		sb.append( " UPLINK:" + getReceiveChannelNumber() );
		
		return sb.toString();
	}
	
	public String getLocationRegistrationArea()
	{
		return mMessage.getHex( LRA, 2 );
	}
	
	public String getRFSubsystemID()
	{
		return mMessage.getHex( RFSS_ID, 2 );
	}
	
	public String getSiteID()
	{
		return mMessage.getHex( SITE_ID, 2 );
	}
	
	public int getTransmitIdentifier()
	{
		return mMessage.getInt( TRANSMIT_IDENTIFIER );
	}
	
	public int getTransmitChannel()
	{
		return mMessage.getInt( TRANSMIT_CHANNEL );
	}
	
	public String getTransmitChannelNumber()
	{
		return getTransmitIdentifier() + "-" + getTransmitChannel();
	}
	
	public int getReceiveIdentifier()
	{
		return mMessage.getInt( RECEIVE_IDENTIFIER );
	}
	
	public int getReceiveChannel()
	{
		return mMessage.getInt( RECEIVE_CHANNEL );
	}
	
	public String getReceiveChannelNumber()
	{
		return getReceiveIdentifier() + "-" + getReceiveChannel();
	}
}
