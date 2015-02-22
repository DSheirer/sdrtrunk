package decode.p25.message.tdu.lc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decode.p25.message.IBandIdentifier;
import decode.p25.message.IdentifierReceiver;
import decode.p25.message.tsbk.osp.control.SystemService;
import decode.p25.reference.LinkControlOpcode;

public class SecondaryControlChannelBroadcastExplicit 
	extends TDULinkControlMessage implements IdentifierReceiver
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( SecondaryControlChannelBroadcastExplicit.class );

	public static final int[] RFSS_ID = { 72,73,74,75,88,89,90,91 };
	public static final int[] SITE_ID = { 92,93,94,95,96,97,98,99 };
	public static final int[] TRANSMIT_IDENTIFIER = { 112,113,114,115 };
	public static final int[] TRANSMIT_CHANNEL = { 116,117,118,119,120,121,122,
		123,136,137,138,139 };
	public static final int[] RECEIVE_IDENTIFIER = { 140,141,142,143 };
	public static final int[] RECEIVE_CHANNEL = { 144,145,146,147,160,161,162,
		163,164,165,166,167 };
	public static final int[] SYSTEM_SERVICE_CLASS = { 168,169,170,171,184,
		185,186,187 };
	
	private IBandIdentifier mTransmitIdentifierProvider;
	private IBandIdentifier mReceiveIdentifierProvider;
	
	public SecondaryControlChannelBroadcastExplicit( TDULinkControlMessage source )
	{
		super( source );
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
    public void setIdentifierMessage( int identifier, IBandIdentifier message )
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
