package decode.p25.message.tdu.lc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decode.p25.message.IdentifierProvider;
import decode.p25.message.tsbk.osp.control.IdentifierProviderReceiver;
import decode.p25.message.tsbk.osp.control.SystemService;
import decode.p25.reference.LinkControlOpcode;

public class SecondaryControlChannelBroadcast extends TDULinkControlMessage
						implements IdentifierProviderReceiver
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( SecondaryControlChannelBroadcast.class );

	public static final int[] RFSS_ID = { 72,73,74,75,88,89,90,91 };
	public static final int[] SITE_ID = { 92,93,94,95,96,97,98,99 };
	public static final int[] IDENTIFIER_A = { 112,113,114,115 };
	public static final int[] CHANNEL_A = { 116,117,118,119,120,121,122,
		123,136,137,138,139 };
	
	public static final int[] SYSTEM_SERVICE_CLASS_A = { 140,141,142,143,144,
		145,146,147 };
	
	public static final int[] IDENTIFIER_B = { 160,161,162,163 };
	public static final int[] CHANNEL_B = { 164,165,166,167,168,169,170,
		171,184,185,186,187 };
	public static final int[] SYSTEM_SERVICE_CLASS_B = { 188,189,190,191,192,
		193,194,195 };
	
	private IdentifierProvider mIdentifierProviderA;
	private IdentifierProvider mIdentifierProviderB;
	
	public SecondaryControlChannelBroadcast( TDULinkControlMessage source )
	{
		super( source );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.SECONDARY_CONTROL_CHANNEL_BROADCAST.getDescription();
    }

	@Override
	public String getMessage()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getMessageStub() );
		
		sb.append( " SITE:" + getRFSubsystemID() + "-" + getSiteID() );
		
		sb.append( " CHAN A:" + getChannelA() );
		
        sb.append( " " + SystemService.toString( getSystemServiceClassA() ) );
		
		sb.append( " CHAN B:" + getChannelB() );
		
        sb.append( " " + SystemService.toString( getSystemServiceClassB() ) );
		
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
	
	public int getIdentifierA()
	{
		return mMessage.getInt( IDENTIFIER_A );
	}
	
	public int getChannelA()
	{
		return mMessage.getInt( CHANNEL_A );
	}
	
	public String getChannelNumberA()
	{
		return getIdentifierA() + "-" + getChannelA();
	}
	
	public int getIdentifierB()
	{
		return mMessage.getInt( IDENTIFIER_B );
	}
	
	public int getChannelB()
	{
		return mMessage.getInt( CHANNEL_B );
	}
	
	public String getChannelNumberB()
	{
		return getIdentifierB() + "-" + getChannelB();
	}
	
	public int getSystemServiceClassA()
	{
		return mMessage.getInt( SYSTEM_SERVICE_CLASS_A );
	}

	public int getSystemServiceClassB()
	{
		return mMessage.getInt( SYSTEM_SERVICE_CLASS_B );
	}
	
	@Override
    public void setIdentifierMessage( int identifier, IdentifierProvider message )
    {
		if( identifier == getIdentifierA() )
		{
			mIdentifierProviderA = message;
		}
		if( identifier == getIdentifierB() )
		{
			mIdentifierProviderB = message;
		}
    }

	@Override
    public int[] getIdentifiers()
    {
		int[] identifiers = new int[ 2 ];
		
		identifiers[ 0 ] = getIdentifierA();
		identifiers[ 1 ] = getIdentifierB();
		
		return identifiers;
    }
	
    public long getDownlinkFrequencyA()
    {
    	return calculateDownlink( mIdentifierProviderA, getChannelA() );
    }
    
    public long getUplinkFrequencyA()
    {
    	return calculateUplink( mIdentifierProviderA, getChannelA() );
    }
    
    public long getDownlinkFrequencyB()
    {
    	return calculateDownlink( mIdentifierProviderB, getChannelB() );
    }
    
    public long getUplinkFrequencyB()
    {
    	return calculateUplink( mIdentifierProviderB, getChannelB() );
    }
}
