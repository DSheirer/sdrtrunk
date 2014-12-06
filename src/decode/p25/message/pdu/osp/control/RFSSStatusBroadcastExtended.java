package decode.p25.message.pdu.osp.control;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.pdu.PDUMessage;
import decode.p25.message.tsbk.osp.control.IdentifierUpdate;
import decode.p25.message.tsbk.osp.control.IdentifierUpdateReceiver;
import decode.p25.message.tsbk.osp.control.SystemService;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

public class RFSSStatusBroadcastExtended extends PDUMessage 
								implements IdentifierUpdateReceiver
{
	public static final int[] LRA = { 88,89,90,91,92,93,94,95 };
	public static final int ACTIVE_NETWORK_CONNECTION_INDICATOR = 99;
	public static final int[] SYSTEM_ID = { 100,101,102,103,104,105,106,107,108,
		109,110,111 };
	public static final int[] RFSS_ID = { 160,161,162,163,164,165,166,167 };
	public static final int[] SITE_ID = { 168,169,170,171,172,173,174,175 };
	public static final int[] TRANSMIT_IDENTIFIER = { 184,185,186,187 };
	public static final int[] TRANSMIT_CHANNEL = { 188,189,190,191,192,193,194,
		195,196,197,198,199 };
	public static final int[] RECEIVE_IDENTIFIER = { 200,201,202,203 };
	public static final int[] RECEIVE_CHANNEL = { 204,205,206,207,208,209,210,
		211,212,213,214,215 };
	public static final int[] SYSTEM_SERVICE_CLASS = { 216,217,218,219,220,221,
		222,223 };
	public static final int[] MULTIPLE_BLOCK_CRC = { 224,225,226,227,228,229,
		230,231,232,233,234,235,236,237,238,239,240,241,242,243,244,245,246,247,
		248,249,250,251,252,253,254,255 };
	
	private IdentifierUpdate mTransmitIdentifierUpdate;
	private IdentifierUpdate mReceiveIdentifierUpdate;
	
	public RFSSStatusBroadcastExtended( BitSetBuffer message,
            DataUnitID duid, AliasList aliasList )
    {
	    super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.NETWORK_STATUS_BROADCAST.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        sb.append( " LRA:" + getLocationRegistrationArea() );
        
        sb.append( " SYS:" + getSystemID() );
        
        sb.append( " SITE:" + getRFSubsystemID() + "-" + getSiteID() );
        
        sb.append( " CTRL CHAN:" + getReceiveIdentifier() + "-" + getReceiveChannel() );
        
        sb.append( " DN:" + getDownlinkFrequency() );
        
        sb.append( " " + getTransmitIdentifier() + "-" + getTransmitChannel() );
        
        sb.append( " UP:" + getUplinkFrequency() );
        
        sb.append( " SYS SVC CLASS:" + 
                SystemService.toString( getSystemServiceClass() ) );
        
        return sb.toString();
    }
    
	
	public String getLocationRegistrationArea()
	{
		return mMessage.getHex( LRA, 2 );
	}
	
	public boolean hasActiveNetworkConnection()
	{
		return mMessage.get( ACTIVE_NETWORK_CONNECTION_INDICATOR );
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
	
	public int getTransmitIdentifier()
	{
		return mMessage.getInt( TRANSMIT_IDENTIFIER );
	}
	
	public int getTransmitChannel()
	{
		return mMessage.getInt( TRANSMIT_CHANNEL );
	}
	
	public int getReceiveIdentifier()
	{
		return mMessage.getInt( RECEIVE_IDENTIFIER );
	}
	
	public int getReceiveChannel()
	{
		return mMessage.getInt( RECEIVE_CHANNEL );
	}
	
    public int getSystemServiceClass()
    {
        return mMessage.getInt( SYSTEM_SERVICE_CLASS );
    }
    
    public long getDownlinkFrequency()
    {
    	return calculateDownlink( mTransmitIdentifierUpdate, getTransmitChannel() );
    }
    
    public long getUplinkFrequency()
    {
    	return calculateUplink( mReceiveIdentifierUpdate, getReceiveChannel() );
    }

	@Override
    public void setIdentifierMessage( int identifier, IdentifierUpdate message )
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
	
	public int[] getIdentifiers()
	{
		int[] idens = new int[ 2 ];
		
		idens[ 0 ] = getTransmitIdentifier();
		idens[ 1 ] = getReceiveIdentifier();
		
		return idens;
	}
}
