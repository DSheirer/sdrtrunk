package io.github.dsheirer.module.decode.p25.message.tsbk.osp.control;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Opcode;
import io.github.dsheirer.alias.AliasList;

public class AcknowledgeResponse extends TSBKMessage
{
    public static final int ADDITIONAL_INFORMATION_FLAG = 80;
    public static final int EXTENDED_ADDRESS_FLAG = 81;
    public static final int[] SERVICE_TYPE = { 82,83,84,85,86,87 };
    public static final int[] WACN = { 88,89,90,91,92,93,94,95,96,97,98,99,100,
        101,102,103,104,105,106,107 };
    public static final int[] SYSTEM_ID = { 108,109,110,111,112,113,114,115,116,
        117,118,119 };
    public static final int[] SOURCE_ADDRESS = { 96,97,98,99,100,101,102,103,
        104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119 };
    public static final int[] TARGET_ADDRESS = { 120,121,122,123,124,125,126,
        127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143 };
    
    public AcknowledgeResponse( BinaryMessage message, 
                                DataUnitID duid,
                                AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.ACKNOWLEDGE_RESPONSE.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        sb.append( " SVC TYPE: " + getServiceType() );
        
        if( hasAdditionalInformation() )
        {
            if( hasExtendedAddress() )
            {
                sb.append( " WACN: " + getWACN() );
                sb.append( " SYS ID: " + getSystemID() );
            }
            else
            {
                sb.append( " SRC: " + getSourceAddress() );
            }
        }
        
        return sb.toString();
    }
    
    public boolean hasAdditionalInformation()
    {
        return mMessage.get( ADDITIONAL_INFORMATION_FLAG );
    }
    
    public boolean hasExtendedAddress()
    {
        return mMessage.get( EXTENDED_ADDRESS_FLAG );
    }

    public String getServiceType()
    {
        return mMessage.getHex( SERVICE_TYPE, 2 );
    }
    
    public String getWACN()
    {
        if( hasAdditionalInformation() && !hasExtendedAddress() )
        {
            return mMessage.getHex( WACN, 5 );
        }
        
        return null;
    }
    
    public String getSystemID()
    {
        if( hasAdditionalInformation() && !hasExtendedAddress() )
        {
            return mMessage.getHex( SYSTEM_ID, 3 );
        }
        
        return null;
    }
    
    public String getSourceAddress()
    {
        if( hasAdditionalInformation() && !hasExtendedAddress() )
        {
            return mMessage.getHex( SOURCE_ADDRESS, 6 );
        }
        
        return null;
    }
    
    @Override
    public String getFromID()
    {
        return getSourceAddress();
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
