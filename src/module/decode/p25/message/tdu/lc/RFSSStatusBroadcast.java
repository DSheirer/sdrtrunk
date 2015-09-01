package module.decode.p25.message.tdu.lc;

import module.decode.p25.message.IBandIdentifier;
import module.decode.p25.message.IdentifierReceiver;
import module.decode.p25.message.tsbk.osp.control.SystemService;
import module.decode.p25.reference.LinkControlOpcode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RFSSStatusBroadcast extends TDULinkControlMessage 
								 implements IdentifierReceiver
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( RFSSStatusBroadcast.class );

	public static final int[] LRA = { 72,73,74,75,88,89,
		90,91 };
	public static final int[] SYSTEM_ID = { 96,97,98,99,112,113,114,115,116,117,
		118,119 };
	public static final int[] RFSS_ID = { 120,121,122,123,136,137,138,
		139 };
	public static final int[] SITE_ID = { 140,141,142,143,144,145,146,147 };
	
	public static final int[] IDENTIFIER = { 160,161,162,163 };
	public static final int[] CHANNEL = { 164,165,166,167,168,169,170,
		171,184,185,186,187 };
	public static final int[] SYSTEM_SERVICE_CLASS = { 188,189,190,191,192,193,
		194,195 };
	
	private IBandIdentifier mIdentifierUpdate;
	
	public RFSSStatusBroadcast( TDULinkControlMessage source )
	{
		super( source );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.RFSS_STATUS_BROADCAST.getDescription();
    }

	@Override
	public String getMessage()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getMessageStub() );
		
		sb.append( " LRA:" + getLocationRegistrationArea() );
		
		sb.append( " SYS:" + getSystem() );

		sb.append( " SITE:" + getRFSubsystemID() + "-" + getSiteID() );
		
		sb.append( " CHAN:" + getChannelNumber() );
		
        sb.append( " " + SystemService.toString( getSystemServiceClass() ) );
		
        sb.append( " " + mMessage.toString() );
        
		return sb.toString();
	}
	
	public String getLocationRegistrationArea()
	{
		return mMessage.getHex( LRA, 2 );
	}
	
	public String getSystem()
	{
		return mMessage.getHex( SYSTEM_ID, 3 );
	}
	
	public String getRFSubsystemID()
	{
		return mMessage.getHex( RFSS_ID, 2 );
	}
	
	public String getSiteID()
	{
		return mMessage.getHex( SITE_ID, 2 );
	}
	
	public int getIdentifier()
	{
		return mMessage.getInt( IDENTIFIER );
	}
	
	public String getChannel()
	{
		return getIdentifier() + "-" + getChannelNumber();
	}
	
	public int getChannelNumber()
	{
		return mMessage.getInt( CHANNEL );
	}
	
	public int getSystemServiceClass()
	{
		return mMessage.getInt( SYSTEM_SERVICE_CLASS );
	}

	@Override
    public void setIdentifierMessage( int identifier, IBandIdentifier message )
    {
	    mIdentifierUpdate = message;
    }

	@Override
    public int[] getIdentifiers()
    {
		int[] identifiers = new int[ 1 ];
		
		identifiers[ 0 ] = getIdentifier();
		
	    return identifiers;
    }
	
    public long getDownlinkFrequency()
    {
    	return calculateDownlink( mIdentifierUpdate, getChannelNumber() );
    }
    
    public long getUplinkFrequency()
    {
    	return calculateUplink( mIdentifierUpdate, getChannelNumber() );
    }
	
}
