package module.decode.p25.message.tdu.lc;

import module.decode.p25.message.IBandIdentifier;
import module.decode.p25.message.IdentifierReceiver;
import module.decode.p25.reference.LinkControlOpcode;
import module.decode.p25.reference.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkStatusBroadcast extends TDULinkControlMessage
									implements IdentifierReceiver
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( NetworkStatusBroadcast.class );
	
	public static final int[] WACN = { 72,73,74,75,88,89,90,91,92,93,94,95,96,
		97,98,99,112,113,114,115,116,117,118,119,120,121,122,123 };
	public static final int[] SYSTEM = { 136,137,138,139,140,141,142,143,144,
		145,146,147 };
	public static final int[] IDENTIFIER = { 160,161,162,163 };
	public static final int[] CHANNEL = { 164,165,166,167,168,169,170,171,184,
		185,186,187 };
	public static final int[] SYSTEM_SERVICE_CLASS = { 188,189,190,191,192,193,
		194,195 };
	
	private IBandIdentifier mIdentifierUpdate;
	
	public NetworkStatusBroadcast( TDULinkControlMessage source )
	{
		super( source );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.NETWORK_STATUS_BROADCAST.getDescription();
    }

	@Override
	public String getMessage()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getMessageStub() );

		sb.append( " NETWORK:" + getWACN() );
		sb.append( " SYS:" + getSystem() );
		sb.append( " CHAN:" + getChannel() );
		sb.append( " " + Service.getServices( getSystemServiceClass() ).toString() );
		
		return sb.toString();
	}
	
	public String getWACN()
	{
		return mMessage.getHex( WACN, 5 );
	}
	
	public String getSystem()
	{
		return mMessage.getHex( SYSTEM, 3 );
	}

	public int getIdentifier()
	{
		return mMessage.getInt( IDENTIFIER );
	}
	
	public int getChannelNumber()
	{
		return mMessage.getInt( CHANNEL );
	}
	
	public String getChannel()
	{
		return getIdentifier() + "-" + getChannelNumber();
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
