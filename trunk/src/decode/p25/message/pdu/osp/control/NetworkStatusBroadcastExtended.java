package decode.p25.message.pdu.osp.control;

import alias.AliasList;
import bits.BinaryMessage;
import decode.p25.message.IBandIdentifier;
import decode.p25.message.IdentifierReceiver;
import decode.p25.message.pdu.PDUMessage;
import decode.p25.message.tsbk.osp.control.SystemService;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;
import decode.p25.reference.P25NetworkCallsign;
import edac.CRCP25;

public class NetworkStatusBroadcastExtended extends PDUMessage 
								implements IdentifierReceiver
{
	public static final int[] LRA = { 88,89,90,91,92,93,94,95 };
	public static final int[] SYSTEM_ID = { 100,101,102,103,104,105,106,107,108,
		109,110,111 };
	public static final int[] WACN = { 160,161,162,163,164,165,166,167,168,169,
		170,171,172,173,174,175,176,177,178,179 };
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
	
	private IBandIdentifier mTransmitIdentifierUpdate;
	private IBandIdentifier mReceiveIdentifierUpdate;
	
	public NetworkStatusBroadcastExtended( BinaryMessage message,
            DataUnitID duid, AliasList aliasList )
    {
	    super( message, duid, aliasList );
	    
	    /* Header block is already error detected/corrected - perform error
	     * detection correction on the intermediate and final data blocks */
	    mMessage = CRCP25.correctPDU1( mMessage );
	    mCRC[ 1 ] = mMessage.getCRC();
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
        
        sb.append( " WACN:" + getWACN() );
        
        sb.append( " SYS:" + getSystemID() );
        
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
	
	public String getSystemID()
	{
		return mMessage.getHex( SYSTEM_ID, 3 );
	}
	
	public String getWACN()
	{
		return mMessage.getHex( WACN, 5 );
	}
	
	public String getNetworkCallsign()
	{
		return P25NetworkCallsign.getCallsign( mMessage.getInt( WACN ), 
											   mMessage.getInt( SYSTEM_ID ) );
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
    public void setIdentifierMessage( int identifier, IBandIdentifier message )
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
