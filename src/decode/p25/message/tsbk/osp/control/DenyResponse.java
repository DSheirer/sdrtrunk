package decode.p25.message.tsbk.osp.control;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.DenyReason;
import decode.p25.reference.Opcode;

public class DenyResponse extends TSBKMessage
{
    public static final int ADDITIONAL_INFORMATION_FLAG = 80;
    public static final int[] SERVICE_TYPE = { 82,83,84,85,86,87 };
    public static final int[] REASON_CODE = { 88,89,90,91,92,93,94,95 };

    /* Additional Info block not yet parsed */
    
    public static final int[] TARGET_ADDRESS = { 120,121,122,123,124,125,126,
        127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143 };
    
    public DenyResponse( BitSetBuffer message, 
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
    
    public String getServiceType()
    {
        return mMessage.getHex( SERVICE_TYPE, 2 );
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
