package decode.p25.message.tsbk.osp.data;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.tsbk.osp.control.IdentifierUpdate;
import decode.p25.message.tsbk.osp.control.IdentifierUpdateReceiver;
import decode.p25.reference.DataUnitID;

public class SNDCPDataChannelAnnouncementExplicit extends SNDCPData 
					implements IdentifierUpdateReceiver
{
	public static final int AUTONOMOUS_ACCESS_AVAILABLE_INDICATOR = 88;
	public static final int REQUESTED_ACCESS_AVAILABLE_INDICATOR = 89;
	
	public static final int[] TRANSMIT_IDENTIFIER = { 96,97,98,99 };
    public static final int[] TRANSMIT_CHANNEL = { 100,101,102,103,104,105,106,
    	107,108,109,110,111 };
	public static final int[] RECEIVE_IDENTIFIER = { 112,113,114,115 };
    public static final int[] RECEIVE_CHANNEL = { 116,117,118,119,120,121,122,
    	123,124,125,126,127 };
    
    public static final int[] DATA_ACCESS_CONTROL = { 128,129,130,131,132,133,
    	134,135,136,137,138,139,140,141,142,143 };
    
    private IdentifierUpdate mIdentifierUpdateTransmit;
    private IdentifierUpdate mIdentifierUpdateReceive;

    public SNDCPDataChannelAnnouncementExplicit( BitSetBuffer message, 
								  DataUnitID duid,
								  AliasList aliasList )
    {
	    super( message, duid, aliasList );
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );
        
        if( isEmergency() )
        {
            sb.append( " EMERGENCY" );
        }
        
        if( isAutonomousAccessAvailable() )
        {
        	sb.append( " AUTONOMOUS ACCESS" );
        }
        
        if( isRequestedAccessAvailable() )
        {
            sb.append( " DOWNLINK:" );
            sb.append( getTransmitIdentifier() + "-" + getTransmitChannel() );
            
            sb.append( " " + getUplinkFrequency() );
            
            sb.append( " UPLINK:" );
            sb.append( getReceiveIdentifier() + "-" + getReceiveChannel() );
            
            sb.append( " " + getDownlinkFrequency() );
            
            sb.append( " DATA ACCESS CONTROL:" );
        }
        else
        {
        	sb.append( " ACCESS DENIED TO DATA ACCESS CONTROL:" ); 
        }
        
        sb.append( getDataAccessControl() );
        
        return sb.toString();
    }
    
    public boolean isAutonomousAccessAvailable()
    {
    	return mMessage.get( AUTONOMOUS_ACCESS_AVAILABLE_INDICATOR );
    }
    
    public boolean isRequestedAccessAvailable()
    {
    	return mMessage.get( REQUESTED_ACCESS_AVAILABLE_INDICATOR );
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
    
    public String getDataAccessControl()
    {
    	return mMessage.getHex( DATA_ACCESS_CONTROL, 4 );
    }
    
	@Override
    public void setIdentifierMessage( int identifier, IdentifierUpdate message )
    {
		if( identifier == getTransmitIdentifier() )
		{
			mIdentifierUpdateTransmit = message;
		}
		else if( identifier == getReceiveIdentifier() )
		{
			mIdentifierUpdateReceive = message;
		}
		else
		{
			throw new IllegalArgumentException( "unexpected channel band "
					+ "identifier [" + identifier + "]" );
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
    	return calculateDownlink( mIdentifierUpdateTransmit, getReceiveChannel() );
    }
    
    public long getUplinkFrequency()
    {
    	return calculateUplink( mIdentifierUpdateReceive, getTransmitChannel() );
    }
}
