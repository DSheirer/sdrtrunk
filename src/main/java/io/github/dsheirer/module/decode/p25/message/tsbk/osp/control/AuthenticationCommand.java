package io.github.dsheirer.module.decode.p25.message.tsbk.osp.control;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.module.decode.p25.reference.Opcode;

public class AuthenticationCommand extends TSBKMessage
{
    public static final int[] WACN = { 88,89,90,91,92,93,94,95,96,97,98,99,100,
        101,102,103,104,105,106,107 };
    public static final int[] SYSTEM_ID = { 108,109,110,111,112,113,114,115,116,
        117,118,119 };
    public static final int[] TARGET_ID = { 120,121,122,123,124,125,126,
        127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143 };
    
    public AuthenticationCommand( BinaryMessage message, 
                                DataUnitID duid,
                                AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.AUTHENTICATION_COMMAND.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        sb.append( " WACN: " + getWACN() );
        sb.append( " SYS ID: " + getSystemID() );
        sb.append( " TGT ID: " + getTargetID() );
        
        return sb.toString();
    }
    
    public String getWACN()
    {
        return mMessage.getHex( WACN, 5 );
    }
    
    public String getSystemID()
    {
        return mMessage.getHex( SYSTEM_ID, 3 );
    }
    
    public String getTargetID()
    {
        return mMessage.getHex( TARGET_ID, 6 );
    }
    
    public String getFullTargetID()
    {
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append( getWACN() );
    	sb.append( "-" );
    	sb.append( getSystemID() );
    	sb.append( "-" );
    	sb.append( getTargetID() );
    	
    	return sb.toString();
    }

    @Override
    public String getToID()
    {
        return getTargetID();
    }
}
