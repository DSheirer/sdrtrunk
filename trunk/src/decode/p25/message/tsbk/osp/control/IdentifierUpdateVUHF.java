package decode.p25.message.tsbk.osp.control;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

public class IdentifierUpdateVUHF extends IdentifierUpdate
{
    public static final int[] BANDWIDTH = { 84,85,86,87 };

    public static final int TRANSMIT_OFFSET_VHF_UHF_SIGN = 88;

    public static final int[] TRANSMIT_OFFSET = { 89,90,91,92,93,94,95,96,97,98,
    	99,100,101 };
    
    public IdentifierUpdateVUHF( BitSetBuffer message, 
                             DataUnitID duid,
                             AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }
    
    @Override
    public String getEventType()
    {
    	return Opcode.IDENTIFIER_UPDATE_VHF_UHF_BANDS.getDescription();
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
        int bandwidth = mMessage.getInt( BANDWIDTH );

        switch( bandwidth )
        {
            case 4:
                return 6250;
            case 5:
                return 12500;
            default:
                return 0;
        }
    }

    /**
     * Transmit offset in hertz
     */
    @Override
    public long getTransmitOffset()
    {
        long offset = mMessage.getLong( TRANSMIT_OFFSET ) * 
        		getChannelSpacing();
        
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
