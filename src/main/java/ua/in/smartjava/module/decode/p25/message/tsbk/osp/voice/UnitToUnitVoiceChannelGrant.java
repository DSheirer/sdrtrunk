package ua.in.smartjava.module.decode.p25.message.tsbk.osp.voice;

import ua.in.smartjava.module.decode.p25.message.tsbk.UnitChannelGrant;
import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.module.decode.p25.reference.Opcode;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;

public class UnitToUnitVoiceChannelGrant extends UnitChannelGrant
{
    public UnitToUnitVoiceChannelGrant( BinaryMessage message, 
                                   DataUnitID duid,
                                   AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.UNIT_TO_UNIT_VOICE_CHANNEL_GRANT.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );
        
        if( isEmergency() )
        {
            sb.append( " EMERGENCY" );
        }
        
        sb.append( " SOURCE UNIT:" );
        sb.append( getSourceAddress() );
        
        sb.append( " TARGET UNIT:" );
        sb.append( getTargetAddress() );

        sb.append( " CHAN:" + getChannelIdentifier() + "-" + getChannelNumber() );
        sb.append( " DN:" + getDownlinkFrequency() );
        sb.append( " UP:" + getUplinkFrequency() );
        
        return sb.toString();
    }
}
