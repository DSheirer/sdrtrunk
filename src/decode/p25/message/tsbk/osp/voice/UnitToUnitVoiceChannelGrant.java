package decode.p25.message.tsbk.osp.voice;

import alias.AliasList;
import bits.BinaryMessage;
import decode.p25.message.tsbk.UnitChannelGrant;
import decode.p25.message.tsbk.osp.control.IdentifierUpdate;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

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
