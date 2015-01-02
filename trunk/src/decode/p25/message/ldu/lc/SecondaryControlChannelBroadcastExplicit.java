package decode.p25.message.ldu.lc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decode.p25.message.IdentifierProvider;
import decode.p25.message.IdentifierReceiver;
import decode.p25.message.ldu.LDU1Message;
import decode.p25.message.tsbk.osp.control.SystemService;
import decode.p25.reference.LinkControlOpcode;

public class SecondaryControlChannelBroadcastExplicit 
	extends LDU1Message implements IdentifierReceiver
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( SecondaryControlChannelBroadcastExplicit.class );

	public static final int[] RFSS_ID = { 364,365,366,367,372,373,374,375 };
	public static final int[] SITE_ID = { 376,377,382,383,384,385,386,387 };
	public static final int[] TRANSMIT_IDENTIFIER = { 536,537,538,539 };
	public static final int[] TRANSMIT_CHANNEL = { 540,541,546,547,548,549,550,
		551,556,557,558,559 };
	public static final int[] RECEIVE_IDENTIFIER = { 560,561,566,567 };
	public static final int[] RECEIVE_CHANNEL = { 568,569,570,571,720,721,722,
		723,724,725,730,731 };
	public static final int[] SYSTEM_SERVICE_CLASS = { 732,733,734,735,740,741,
		742,743 };
	
	private IdentifierProvider mTransmitIdentifierProvider;
	private IdentifierProvider mReceiveIdentifierProvider;
	
	public SecondaryControlChannelBroadcastExplicit( LDU1Message message )
	{
		super( message );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT.getDescription();
    }

	@Override
	public String getMessage()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getMessageStub() );
		
		sb.append( " SITE:" + getRFSubsystemID() + "-" + getSiteID() );
		
		sb.append( " TRANSMIT:" + getTransmitChannel() );

		sb.append( " RECEIVE:" + getReceiveChannel() );
		
        sb.append( " " + SystemService.toString( getSystemServiceClass() ) );
		
		return sb.toString();
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
	
	public int getTransmitChannelNumber()
	{
		return mMessage.getInt( TRANSMIT_CHANNEL );
	}
	
	public String getTransmitChannel()
	{
		return getTransmitIdentifier() + "-" + getTransmitChannelNumber();
	}
	
	public int getReceiveIdentifier()
	{
		return mMessage.getInt( RECEIVE_IDENTIFIER );
	}
	
	public int getReceiveChannelNumber()
	{
		return mMessage.getInt( RECEIVE_CHANNEL );
	}
	
	public String getReceiveChannel()
	{
		return getReceiveIdentifier() + "-" + getReceiveChannelNumber();
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
			mTransmitIdentifierProvider = message;
		}
		if( identifier == getReceiveIdentifier() )
		{
			mReceiveIdentifierProvider = message;
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
    	return calculateDownlink( mTransmitIdentifierProvider, 
    						getTransmitChannelNumber() );
    }
    
    public long getUplinkFrequency()
    {
    	return calculateUplink( mReceiveIdentifierProvider, 
    			getReceiveChannelNumber() );
    }
}
