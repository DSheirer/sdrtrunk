package decode.p25.message.tsbk;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.tsbk.osp.control.IdentifierUpdate;
import decode.p25.reference.DataUnitID;

public abstract class GroupChannelGrantExplicit extends ChannelGrant
{
    public static final int[] TRANSMIT_CHANNEL_ID = { 96,97,98,99 };
    public static final int[] TRANSMIT_CHANNEL_NUMBER = { 100,101,102,103,
        104,105,106,107,108,109,110,111 };
    public static final int[] RECEIVE_CHANNEL_ID = { 112,113,114,115 };
    public static final int[] RECEIVE_CHANNEL_NUMBER = { 116,117,118,119,
        120,121,122,123,124,125,126,127 };
    public static final int[] GROUP_ADDRESS = { 128,129,130,131,132,133,134,135,
        136,137,138,139,140,141,142,143 };
    
    private IdentifierUpdate mTransmitIdentifierUpdate;
    private IdentifierUpdate mReceiveIdentifierUpdate;

    public GroupChannelGrantExplicit( BitSetBuffer message, 
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
        
        sb.append( " XMIT:" );
        sb.append( getTransmitChannelID() + "/" + getTransmitChannelNumber() );
        
        sb.append( " RCV:" );
        sb.append( getReceiveChannelID() + "/" + getReceiveChannelNumber() );
        
        sb.append( " GRP:" );
        sb.append( getGroupAddress() );
        
        return sb.toString();
    }
    
    public int getTransmitChannelID()
    {
        return mMessage.getInt( TRANSMIT_CHANNEL_ID );
    }
    
    public int getTransmitChannelNumber()
    {
        return mMessage.getInt( TRANSMIT_CHANNEL_NUMBER );
    }
    
    public int getReceiveChannelID()
    {
        return mMessage.getInt( RECEIVE_CHANNEL_ID );
    }
    
    public int getReceiveChannelNumber()
    {
        return mMessage.getInt( RECEIVE_CHANNEL_NUMBER );
    }
    
    public String getGroupAddress()
    {
        return mMessage.getHex( GROUP_ADDRESS, 4 );
    }
    
	@Override
    public void setIdentifierMessage( int identifier, IdentifierUpdate message )
    {
		if( identifier == getTransmitChannelID() )
		{
			mTransmitIdentifierUpdate = message;
		}
		
		if( identifier == getReceiveChannelID() )
		{
			mReceiveIdentifierUpdate = message;
		}
    }

	@Override
    public int[] getIdentifiers()
    {
		int[] identifiers = new int[ 2 ];
		identifiers[ 0 ] = getTransmitChannelID();
		identifiers[ 1 ] = getReceiveChannelID();
		
		return identifiers;
    }
    
    public long getDownlinkFrequency()
    {
    	return calculateDownlink( mTransmitIdentifierUpdate, getChannel() );
    }
    
    public long getUplinkFrequency()
    {
    	return calculateUplink( mReceiveIdentifierUpdate, getChannel() );
    }
}
