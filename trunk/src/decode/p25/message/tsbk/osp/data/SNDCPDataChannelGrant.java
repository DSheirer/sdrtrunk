package decode.p25.message.tsbk.osp.data;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.tsbk.osp.control.IdentifierUpdate;
import decode.p25.message.tsbk.osp.control.IdentifierUpdateReceiver;
import decode.p25.reference.DataUnitID;

public class SNDCPDataChannelGrant extends SNDCPData implements IdentifierUpdateReceiver
{
	public static final int[] TRANSMIT_IDENTIFIER = { 88,89,90,91 };
    public static final int[] TRANSMIT_CHANNEL = { 92,93,94,95,96,97,98,99,100,
    	101,102,103 };
	public static final int[] RECEIVE_IDENTIFIER = { 104,105,106,107 };
    public static final int[] RECEIVE_CHANNEL = { 108,109,110,111,112,113,114,
    	115,116,117,118,119 };
    
    public static final int[] TARGET_ADDRESS = { 120,121,122,123,124,125,126,
        127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143 };
    
    private IdentifierUpdate mIdentifierUpdateTransmit;
    private IdentifierUpdate mIdentifierUpdateReceive;

    public SNDCPDataChannelGrant( BitSetBuffer message, 
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
        
        sb.append( " UPLINK:" );
        sb.append( getTransmitIdentifier() + "-" + getTransmitChannel() );
        
        sb.append( " " + getUplinkFrequency() );
        
        sb.append( " DOWNLINK:" );
        sb.append( getReceiveIdentifier() + "-" + getReceiveChannel() );
        
        sb.append( " " + getDownlinkFrequency() );

        sb.append( " TGT:" );
        sb.append( getTargetAddress() );
        
        return sb.toString();
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
    
    public String getTargetAddress()
    {
        return mMessage.getHex( TARGET_ADDRESS, 6 );
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
    	return calculateDownlink( mIdentifierUpdateReceive, getReceiveChannel() );
    }
    
    public long getUplinkFrequency()
    {
    	return calculateUplink( mIdentifierUpdateTransmit, getTransmitChannel() );
    }

	
}
