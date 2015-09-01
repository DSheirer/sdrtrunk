package module.decode.p25.message.tsbk.motorola;

import module.decode.p25.reference.DataUnitID;
import alias.AliasList;
import bits.BinaryMessage;

public class PlannedControlChannnelShutdown extends MotorolaTSBKMessage 
{
    public PlannedControlChannnelShutdown( BinaryMessage message, 
    							   DataUnitID duid,
    							   AliasList aliasList ) 
    {
    	super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return MotorolaOpcode.CCH_PLANNED_SHUTDOWN.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        return sb.toString();
    }
}    
