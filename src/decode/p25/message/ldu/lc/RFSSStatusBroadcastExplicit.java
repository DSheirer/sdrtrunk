package decode.p25.message.ldu.lc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decode.p25.message.IdentifierProvider;
import decode.p25.message.IdentifierReceiver;
import decode.p25.message.ldu.LDU1Message;
import decode.p25.message.tsbk.osp.control.SystemService;
import decode.p25.reference.LinkControlOpcode;

public class RFSSStatusBroadcastExplicit extends LDU1Message 
								 implements IdentifierReceiver
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( RFSSStatusBroadcastExplicit.class );

	public static final int[] LRA = { 364,365,366,367,372,373,374,375 };
	public static final int[] RECEIVE_IDENTIFIER = { 376,377,382,383 };
	public static final int[] RECEIVE_CHANNEL = { 384,385,386,387,536,537,538,
		539,540,541,546,547 };
	public static final int[] RFSS_ID = { 548,549,550,551,556,557,558,559 };
	public static final int[] SITE_ID = { 560,561,566,567,568,569,570,571 };
	public static final int[] TRANSMIT_IDENTIFIER = { 720,721,722,723 };
	public static final int[] TRANSMIT_CHANNEL = { 724,725,730,731,732,733,734,
		735,740,741,742,743 };
	public static final int[] SYSTEM_SERVICE_CLASS = { 744,745,750,751,752,753,754,755 };

	private IdentifierProvider mTransmitIdentifierUpdate;
	private IdentifierProvider mReceiveIdentifierUpdate;
	
	public RFSSStatusBroadcastExplicit( LDU1Message source )
	{
		super( source );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.RFSS_STATUS_BROADCAST_EXPLICIT.getDescription();
    }

	@Override
	public String getMessage()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getMessageStub() );
		
		sb.append( " LRA:" + getLocationRegistrationArea() );
		
		sb.append( " SITE:" + getRFSubsystemID() + "-" + getSiteID() );
		
		sb.append( " TRANSMIT:" + getTransmitChannelNumber() );
		
		sb.append( " RECEIVE:" + getReceiveChannelNumber() );
		
        sb.append( " " + SystemService.toString( getSystemServiceClass() ) );
		
        sb.append( " " + mMessage.toString() );
        
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
	
	public String getTransmitChannel()
	{
		return getTransmitIdentifier() + "-" + getTransmitChannelNumber();
	}
	
	public int getTransmitChannelNumber()
	{
		return mMessage.getInt( TRANSMIT_CHANNEL );
	}
	
	public int getReceiveIdentifier()
	{
		return mMessage.getInt( RECEIVE_IDENTIFIER );
	}
	
	public String getReceiveChannel()
	{
		return getReceiveIdentifier() + "-" + getReceiveChannelNumber();
	}
	
	public int getReceiveChannelNumber()
	{
		return mMessage.getInt( RECEIVE_CHANNEL );
	}
	
	public int getSystemServiceClass()
	{
		return mMessage.getInt( SYSTEM_SERVICE_CLASS );
	}

	@Override
    public void setIdentifierMessage( int identifier, IdentifierProvider message )
    {
		if( identifier == getTransmitIdentifier() )
		{
			mTransmitIdentifierUpdate = message;
		}
		
		if( identifier == getReceiveIdentifier() )
		{
			mReceiveIdentifierUpdate = message;
		}
    }

	@Override
    public int[] getIdentifiers()
    {
		int[] identifiers = new int[ 2 ];
		
		identifiers[ 0 ] = getTransmitIdentifier();
		identifiers[ 1 ] = getReceiveIdentifier();
		
	    return identifiers;
    }
	
    public long getDownlinkFrequency()
    {
    	return calculateDownlink( mTransmitIdentifierUpdate, getTransmitChannelNumber() );
    }
    
    public long getUplinkFrequency()
    {
    	return calculateUplink( mReceiveIdentifierUpdate, getReceiveChannelNumber() );
    }
	
}
