package decode.p25.message.pdu.osp.data;

import java.util.Date;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.pdu.PDUMessage;
import decode.p25.message.tsbk.osp.control.IdentifierUpdate;
import decode.p25.message.tsbk.osp.control.IdentifierUpdateReceiver;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

public class GroupDataChannelGrantExtended extends PDUMessage 
								implements IdentifierUpdateReceiver
{
    /* Service Options */
    public static final int EMERGENCY_FLAG = 128;
    public static final int ENCRYPTED_CHANNEL_FLAG = 129;
    public static final int DUPLEX_MODE = 130;
    public static final int SESSION_MODE = 131;
	
	public static final int[] SOURCE_ADDRESS = { 88,89,90,91,92,93,94,95,96,97,
		98,99,100,101,102,103,104,105,106,107,108,109,110,111 };
	public static final int[] SERVICE_OPTIONS = { 128,129,130,131,132,133,134,
		135 };
	public static final int[] TRANSMIT_IDENTIFIER = { 160,161,162,163 };
	public static final int[] TRANSMIT_CHANNEL = { 164,165,166,167,168,169,170,
		171,172,173,174,175 };
	public static final int[] RECEIVE_IDENTIFIER = { 176,177,178,179 };
	public static final int[] RECEIVE_CHANNEL = { 180,181,182,183,184,185,186,
		187,188,189,190,191 };
	public static final int[] GROUP_ADDRESS = { 192,193,194,195,196,197,198,199,
		200,201,202,203,204,205,206,207 };
	public static final int[] MULTIPLE_BLOCK_CRC = { 224,225,226,227,228,229,
		230,231,232,233,234,235,236,237,238,239,240,241,242,243,244,245,246,247,
		248,249,250,251,252,253,254,255 };
	
	private IdentifierUpdate mTransmitIdentifierUpdate;
	private IdentifierUpdate mReceiveIdentifierUpdate;
	
	public GroupDataChannelGrantExtended( BitSetBuffer message,
            DataUnitID duid, AliasList aliasList )
    {
	    super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        if( isEmergency() )
        {
            sb.append( " EMERGENCY" );
        }
        
        sb.append( " FROM:" );
        sb.append( getSourceAddress() );
        
        sb.append( " TO:" );
        sb.append( getGroupAddress() );
        
        sb.append( " CHAN DN:" + getTransmitIdentifier() + "-" + getTransmitChannel() );
        sb.append( " " + getDownlinkFrequency() );
        
        sb.append( " CHAN UP:" + getReceiveIdentifier() + "-" + getReceiveChannel() );
        sb.append( " " + getUplinkFrequency() );
        
        return sb.toString();
    }
    
    public boolean isEmergency()
    {
        return mMessage.get( EMERGENCY_FLAG );
    }
    
    public boolean isEncryptedChannel()
    {
        return mMessage.get( ENCRYPTED_CHANNEL_FLAG );
    }
    
    public DuplexMode getDuplexMode()
    {
        return mMessage.get( DUPLEX_MODE ) ? DuplexMode.FULL : DuplexMode.HALF;
    }

    public SessionMode getSessionMode()
    {
        return mMessage.get( SESSION_MODE ) ? 
                SessionMode.CIRCUIT : SessionMode.PACKET;
    }

    public String getSourceAddress()
    {
    	return mMessage.getHex( SOURCE_ADDRESS, 6 );
    }

    public String getGroupAddress()
    {
    	return mMessage.getHex( GROUP_ADDRESS, 4 );
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
