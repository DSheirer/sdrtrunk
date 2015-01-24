package decode.p25.message.tsbk.osp.control;

import alias.AliasList;
import bits.BinaryMessage;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.DenyReason;
import decode.p25.reference.Opcode;

public class DenyResponse extends TSBKMessage
{
    public static final int ADDITIONAL_INFORMATION_FLAG = 80;
    public static final int[] SERVICE_TYPE = { 82,83,84,85,86,87 };
    public static final int[] REASON_CODE = { 88,89,90,91,92,93,94,95 };

    public static final int[] CALL_OPTIONS = { 96,97,98,99,100,101,102,103 };
    public static final int[] SOURCE_GROUP = { 104,105,106,107,108,109,110,111 };
    public static final int[] SOURCE_ADDRESS = { 96,97,98,99,100,101,102,103,104,
    	105,106,107,108,109,110,111 };
    
    public static final int[] TARGET_ADDRESS = { 120,121,122,123,124,125,126,
        127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143 };
    
    public DenyResponse( BinaryMessage message, 
                                DataUnitID duid,
                                AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.DENY_RESPONSE.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        sb.append( " TGT ADDR: " + getTargetAddress() );
        
        sb.append( " REASON:" + getReason().name() );
        
        return sb.toString();
    }
    
    public DenyReason getReason()
    {
        int code = mMessage.getInt( REASON_CODE );
        
        return DenyReason.fromCode( code );
    }
    
    public boolean hasAdditionalInformation()
    {
        return mMessage.get( ADDITIONAL_INFORMATION_FLAG );
    }
    
    public Opcode getServiceType()
    {
        return Opcode.fromValue( mMessage.getInt( SERVICE_TYPE ) );
    }
    
    public String getSourceAddress()
    {
    	switch( getServiceType() )
    	{
    		case GROUP_DATA_CHANNEL_GRANT:
    		case GROUP_VOICE_CHANNEL_GRANT:
    			return mMessage.getHex( SOURCE_GROUP, 4 );
    		case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
    		case INDIVIDUAL_DATA_CHANNEL_GRANT:
    		case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
    			return mMessage.getHex( SOURCE_ADDRESS, 6 );
			default:
				break;
    	}
    	
    	return null;
    }
    
    public String getTargetAddress()
    {
        return mMessage.getHex( TARGET_ADDRESS, 6 );
    }

    @Override
    public String getToID()
    {
        return getTargetAddress();
    }
}
