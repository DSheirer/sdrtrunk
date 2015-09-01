package module.decode.p25.message.tdu.lc;

import module.decode.p25.reference.Encryption;
import module.decode.p25.reference.LinkControlOpcode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtectionParameterBroadcast extends TDULinkControlMessage
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( ProtectionParameterBroadcast.class );

	public final static int[] ALGORITHM_ID = { 112,113,114,115,116,117,118,119 };
	public final static int[] KEY_ID = { 120,121,122,123,136,137,138,139,140,
		141,142,143,144,145,146,147 };
    public static final int[] TARGET_ADDRESS = { 160,161,162,163,164,165,166,
    	167,168,169,170,171,184,185,186,187,188,189,190,190,192,193,194,195 };
	
	public ProtectionParameterBroadcast( TDULinkControlMessage source )
	{
		super( source );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.PROTECTION_PARAMETER_BROADCAST.getDescription();
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        sb.append( " ENCRYPTION:" + getEncryption().name() );
        sb.append( " KEY:" + getEncryptionKey() );
        sb.append( " ADDRESSS:" + getTargetAddress() );
        
        return sb.toString();
    }
    
    public Encryption getEncryption()
    {
    	return Encryption.fromValue( mMessage.getInt( ALGORITHM_ID ) );
    }
    
    public int getEncryptionKey()
    {
    	return mMessage.getInt( KEY_ID );
    }
    
    public String getTargetAddress()
    {
    	return mMessage.getHex( TARGET_ADDRESS, 6 );
    }
}
