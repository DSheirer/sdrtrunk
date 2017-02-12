package module.decode.p25.message.tsbk.osp.control;

import module.decode.p25.reference.DataUnitID;
import module.decode.p25.reference.Opcode;
import alias.AliasList;
import bits.BinaryMessage;

public class IdentifierUpdateNonVUHF extends IdentifierUpdate
{
    public static final int[] BANDWIDTH = { 84,85,86,87,88,89,90,91,92 };

    public static final int TRANSMIT_OFFSET_VHF_UHF_SIGN = 93;

    public static final int[] TRANSMIT_OFFSET = { 94,95,96,97,98,99,100,101 };
    
    public IdentifierUpdateNonVUHF( BinaryMessage message, 
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
        sb.append(toString());

        return sb.toString();
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( " ID:" + getIdentifier() );
        sb.append( " BASE:" + getBaseFrequency() );
        sb.append( " BANDWIDTH:" + getBandwidth() );
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
