package module.decode.p25.message.tsbk.osp.voice;

import module.decode.p25.message.tsbk.UnitChannelGrant;
import module.decode.p25.reference.DataUnitID;
import module.decode.p25.reference.Opcode;
import alias.AliasList;
import bits.BinaryMessage;

public class TelephoneInterconnectVoiceChannelGrant extends UnitChannelGrant
{
    public static final int[] CALL_TIMER = { 96,97,98,99,100,101,102,103,
        104,105,106,107,108,109,110,111 };
    
    public TelephoneInterconnectVoiceChannelGrant( BinaryMessage message, 
                                   DataUnitID duid,
                                   AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );
        
        if( isEmergency() )
        {
            sb.append( " EMERGENCY" );
        }
        
        sb.append( " ADDR:" + getSourceAddress() );
        
        sb.append( " CHAN:" );
        sb.append( getChannel() );
        
        sb.append( " CALL TIMER:" );
        sb.append( getCallTimer() );
        
        sb.append( " SECS TO:" );
        sb.append( getToID() );
        
        return sb.toString();
    }
    
    public String getAddress()
    {
    	return getSourceAddress();
    }
    
    /**
     * Call timer in seconds
     */
    public int getCallTimer()
    {
        int units = mMessage.getInt( CALL_TIMER );
        
        return (int)( units / 10 );
    }
    
    @Override
    public String getEventType()
    {
        return Opcode.TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT.getDescription();
    }
}
