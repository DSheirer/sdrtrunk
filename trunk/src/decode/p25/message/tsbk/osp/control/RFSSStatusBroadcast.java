package decode.p25.message.tsbk.osp.control;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.IdentifierProvider;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

public class RFSSStatusBroadcast extends TSBKMessage implements IdentifierProviderReceiver
{
    public static final int[] LOCATION_REGISTRATION_AREA = { 80,81,82,83,84,85,
        86,87 };
    public static final int ACTIVE_NETWORK_CONNECTION_FLAG = 91;
    public static final int[] SYSTEM_ID = { 92,93,94,95,96,97,98,99,100,101,
        102,103 };
    public static final int[] RFSS_ID = { 104,105,106,107,108,109,110,111 };
    public static final int[] SITE_ID = { 112,113,114,115,116,117,118,119 };
    public static final int[] IDENTIFIER = { 120,121,122,123 };
    public static final int[] CHANNEL = { 124,125,126,127,128,129,130,131,132,
    	133,134,135 };
    public static final int[] SYSTEM_SERVICE_CLASS = { 136,137,138,139,140,141,
        142,143 };
    
    private IdentifierProvider mIdentifierUpdate;
    
    public RFSSStatusBroadcast( BitSetBuffer message, 
                                DataUnitID duid,
                                AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.RFSS_STATUS_BROADCAST.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );
        
        sb.append( " LRA:" + getLocationRegistrationArea() );

        sb.append( " SYSID:" + getSystemID() );

        sb.append( " RFSS:" + getRFSubsystemID() );
        
        sb.append( " SITE:" + getSiteID() );
        
        sb.append( " CHAN:" + getIdentifier() + "-" + getChannel() );
        
        sb.append( " DN:" + getDownlinkFrequency() );
        
        sb.append( " UP:" + getUplinkFrequency() );

        if( hasActiveNetworkConnection() )
        {
            sb.append( " ACTIVE-NETWORK-CONN" );
        }
        
        sb.append( SystemService.toString( getSystemServiceClass() ) );
        
        return sb.toString();
    }
    
    public String getLocationRegistrationArea()
    {
        return mMessage.getHex( LOCATION_REGISTRATION_AREA, 2 );
    }
    
    public boolean hasActiveNetworkConnection()
    {
        return mMessage.get( ACTIVE_NETWORK_CONNECTION_FLAG );
    }
    
    public String getSystemID()
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
    
    public int getChannel()
    {
        return mMessage.getInt( CHANNEL );
    }
    
    public int getSystemServiceClass()
    {
        return mMessage.getInt( SYSTEM_SERVICE_CLASS );
    }
    
    public long getDownlinkFrequency()
    {
    	return calculateDownlink( mIdentifierUpdate, getChannel() );
    }
    
    public long getUplinkFrequency()
    {
    	return calculateUplink( mIdentifierUpdate, getChannel() );
    }

	@Override
    public void setIdentifierMessage( int identifier, IdentifierProvider message )
    {
		/* we're only expecting 1 identifier, so use whatever is received */
		mIdentifierUpdate = message;
    }
	
	public int[] getIdentifiers()
	{
		int[] idens = new int[ 1 ];
		
		idens[ 0 ] = getIdentifier();
		
		return idens;
	}
}
