package decode.p25.message.tsbk.osp.control;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

public class IdentifierUpdateNonVUHF extends IdentifierUpdate
{
    public static final int[] BANDWIDTH = { 84,85,86,87,88,89,90,91,92 };

    public static final int TRANSMIT_OFFSET_VHF_UHF_SIGN = 93;

    public static final int[] TRANSMIT_OFFSET = { 94,95,96,97,98,99,100,101 };
    
    public IdentifierUpdateNonVUHF( BitSetBuffer message, 
                             DataUnitID duid,
                             AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }
    
    @Override
    public String getEventType()
    {
    	return Opcode.IDENTIFIER_UPDATE_NON_VUHF.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        sb.append( " IDEN:" + getIdentifier() );

        sb.append( " BASE:" + getBaseFrequency() );
        
        sb.append( " BW:" + getBandwidth() );
        
        sb.append( " SPACING:" + getChannelSpacing() );
        
        sb.append( " OFFSET:" + getTransmitOffset() );
        
        return sb.toString();
    }

    /**
     * Channel bandwidth in hertz
     */
    public int getBandwidth()
    {
    	return mMessage.getInt( BANDWIDTH ) * 125;
    }

    /**
     * Transmit offset in hertz
     */
    @Override
    public long getTransmitOffset()
    {
        long offset = mMessage.getLong( TRANSMIT_OFFSET ) * 250000l;
        
        if( mMessage.get( TRANSMIT_OFFSET_VHF_UHF_SIGN ) )
        {
            return offset;
        }
        else
        {
            return -offset;
        }
    }
}
