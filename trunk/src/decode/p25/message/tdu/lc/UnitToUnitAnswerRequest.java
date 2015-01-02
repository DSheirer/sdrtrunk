package decode.p25.message.tdu.lc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decode.p25.reference.LinkControlOpcode;

public class UnitToUnitAnswerRequest extends TDULinkControlMessage
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( UnitToUnitAnswerRequest.class );
    /* Service Options */
    public static final int EMERGENCY_FLAG = 92;
    public static final int ENCRYPTED_CHANNEL_FLAG = 93;
    public static final int DUPLEX_MODE = 94;
    public static final int SESSION_MODE = 95;
	public static final int[] TARGET_ADDRESS = { 112,113,114,115,116,117,118,
		119,120,121,122,123,136,137,138,139,140,141,142,143,144,145,146,147 };
	public static final int[] SOURCE_ADDRESS = { 160,161,162,163,164,165,166,
		167,168,169,170,171,184,185,186,187,188,189,190,191,192,193,194,195 };
	
	public UnitToUnitAnswerRequest( TDULinkControlMessage source )
	{
		super( source );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.UNIT_TO_UNIT_ANSWER_REQUEST.getDescription();
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
		
		sb.append( " TO:" + getTargetAddress() );
		
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
    
    public String getTargetAddress()
    {
    	return mMessage.getHex( TARGET_ADDRESS, 6 );
    }
    
    public String getSourceAddress()
    {
    	return mMessage.getHex( SOURCE_ADDRESS, 6 );
    }
}
