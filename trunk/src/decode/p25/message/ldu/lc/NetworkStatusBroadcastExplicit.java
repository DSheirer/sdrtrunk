package decode.p25.message.ldu.lc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decode.p25.message.IdentifierProvider;
import decode.p25.message.IdentifierReceiver;
import decode.p25.message.ldu.LDU1Message;
import decode.p25.reference.LinkControlOpcode;

public class NetworkStatusBroadcastExplicit extends LDU1Message
									implements IdentifierReceiver
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( NetworkStatusBroadcastExplicit.class );
	public static final int[] NETWORK_ID = { 364,365,366,367,372,373,374,375,
		376,377,382,383,384,385,386,387,536,537,538,539 };
	public static final int[] SYSTEM_ID = { 540,541,546,547,548,549,550,551,556,
		557,558,559 };
	public static final int[] TRANSMIT_IDENTIFIER = { 560,561,566,567 };
	public static final int[] TRANSMIT_CHANNEL = { 568,569,570,571,720,721,722,
		723,724,725,730,731 };
	public static final int[] RECEIVE_IDENTIFIER = { 732,733,734,735 };
	public static final int[] RECEIVE_CHANNEL = { 740,741,742,743,744,745,750,
		751,752,753,754,755 };
	
	private IdentifierProvider mTransmitIdentifierUpdate;
	private IdentifierProvider mReceiveIdentifierUpdate;
	
	public NetworkStatusBroadcastExplicit( LDU1Message message )
	{
		super( message );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.NETWORK_STATUS_BROADCAST_EXPLICIT.getDescription();
    }

	@Override
	public String getMessage()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getMessageStub() );

		sb.append( " NETWORK:" + getNetworkID() );
		sb.append( " SYS:" + getSystemID() );
		sb.append( " TRANSMIT:" + getTransmitChannel() );
		sb.append( " RECEIVE:" + getReceiveChannel() );
		
		return sb.toString();
	}
	
	public String getNetworkID()
	{
		return mMessage.getHex( NETWORK_ID, 5 );
	}
	
	public String getSystemID()
	{
		return mMessage.getHex( SYSTEM_ID, 3 );
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
    	return calculateDownlink( mTransmitIdentifierUpdate, 
    				getTransmitChannelNumber() );
    }
    
    public long getUplinkFrequency()
    {
    	return calculateUplink( mReceiveIdentifierUpdate, 
    			getReceiveChannelNumber() );
    }
}
