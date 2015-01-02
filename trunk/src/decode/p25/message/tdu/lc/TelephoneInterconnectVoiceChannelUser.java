package decode.p25.message.tdu.lc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decode.p25.reference.LinkControlOpcode;

public class TelephoneInterconnectVoiceChannelUser extends TDULinkControlMessage
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( TelephoneInterconnectVoiceChannelUser.class );
    /* Service Options */
    public static final int EMERGENCY_FLAG = 92;
    public static final int ENCRYPTED_CHANNEL_FLAG = 93;
    public static final int DUPLEX_MODE = 94;
    public static final int SESSION_MODE = 95;
    
    public static final int[] CALL_TIMER = { 120,121,122,123,136,137,138,139,
    	140,141,142,143,144,145,146,147 };
	public static final int[] ADDRESS = { 160,161,162,163,164,165,166,
		167,168,169,170,171,184,185,186,187,188,189,190,191,192,193,194,195 };
	
	public TelephoneInterconnectVoiceChannelUser( TDULinkControlMessage source )
	{
		super( source );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER.getDescription();
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

		sb.append( " TIMER:" + getCallTimer() + " SECS" );
		
		sb.append( " ADDR:" + getAddress() );
		
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
    
    public String getAddress()
    {
    	return mMessage.getHex( ADDRESS, 6 );
    }
    
    /**
     * Call timer in seconds
     */
    public int getCallTimer()
    {
        int units = mMessage.getInt( CALL_TIMER );
        
        return (int)( units / 10 );
    }
    
}
