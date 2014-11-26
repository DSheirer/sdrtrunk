package decode.p25.message.tsbk.osp.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

public class SecondaryControlChannelBroadcastExplicit
				extends SecondaryControlChannelBroadcast
{
	private final static Logger mLog = LoggerFactory.getLogger( 
			SecondaryControlChannelBroadcastExplicit.class );

	public SecondaryControlChannelBroadcastExplicit( BitSetBuffer message,
            DataUnitID duid, AliasList aliasList )
    {
	    super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );
        
        sb.append( " SITE:" + getRFSS() + "-" + getSiteID() );
        
        sb.append( " DOWNLINK:" + getIdentifier1() + "-" + getChannel1() );
        
        sb.append( " " + getDownlinkFrequency1() );
        
        sb.append( " SVC1:" + 
        		SystemService.toString( getSystemServiceClass1() ) );

        if( hasChannel2() )
        {
            sb.append( " UPLINK:" + getIdentifier2() + "-" + getChannel2() );
            
            sb.append( " " + getUplinkFrequency1() );

            sb.append( " SVC2:" + 
            		SystemService.toString( getSystemServiceClass2() ) );
        }

        return sb.toString();
    }
    

}
