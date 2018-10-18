package io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.UnitChannelGrant;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.message.tsbk2.Opcode;

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
        return Opcode.OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT.toString();
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
