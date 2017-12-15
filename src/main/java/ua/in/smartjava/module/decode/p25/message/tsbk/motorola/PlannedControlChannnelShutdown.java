package ua.in.smartjava.module.decode.p25.message.tsbk.motorola;

import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;

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
