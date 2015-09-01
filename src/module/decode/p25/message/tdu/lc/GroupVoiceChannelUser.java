package module.decode.p25.message.tdu.lc;

import module.decode.p25.reference.LinkControlOpcode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupVoiceChannelUser extends TDULinkControlMessage
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( GroupVoiceChannelUser.class );
    /* Service Options */
    public static final int EMERGENCY_FLAG = 92;
    public static final int ENCRYPTED_CHANNEL_FLAG = 93;
    public static final int DUPLEX_MODE = 94;
    public static final int SESSION_MODE = 95;
	public static final int[] GROUP_ADDRESS = { 120,121,122,123,136,137,138,139,
		140,141,142,143,144,145,146,147 };
	public static final int[] SOURCE_ADDRESS = { 160,161,162,163,164,165,166,
		167,168,169,170,171,184,185,186,187,188,189,190,191,192,193,194,195 };
	
	public GroupVoiceChannelUser( TDULinkControlMessage source )
	{
		super( source );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.GROUP_VOICE_CHANNEL_USER.getDescription();
    }

	@Override
	public String getMessage()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getMessageStub() );
		
		if( isEmergency() )
		{
			sb.append( " EMERGENCY" );
		}
		if( isEncryptedChannel() )
		{
			sb.append( " ENCRYPTED CHANNEL" );
		}

		sb.append( " FROM:" + getSourceAddress() );
		
		sb.append( " TO:" + getGroupAddress() );
		
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
    
    public String getGroupAddress()
    {
    	return mMessage.getHex( GROUP_ADDRESS, 4 );
    }
    
    public String getSourceAddress()
    {
    	return mMessage.getHex( SOURCE_ADDRESS, 6 );
    }
}
