package decode.p25.message.tsbk.motorola;

import alias.AliasList;
import bits.BinaryMessage;
import decode.p25.reference.DataUnitID;

public class CongestionHoldOffTimer extends MotorolaTSBKMessage 
{
    public CongestionHoldOffTimer( BinaryMessage message, 
    							   DataUnitID duid,
    							   AliasList aliasList ) 
    {
    	super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return MotorolaOpcode.CONGESTION_HOLD_OFF_TIMER.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        
        
        return sb.toString();
    }
}    
