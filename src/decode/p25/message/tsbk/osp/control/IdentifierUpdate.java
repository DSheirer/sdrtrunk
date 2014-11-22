package decode.p25.message.tsbk.osp.control;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

public class IdentifierUpdate extends TSBKMessage
{
    public static final int[] CHANNEL_IDENTIFIER = { 80,81,82,83 };

    public static final int[] CHANNEL_SPACING = { 102,103,104,105,106,107,108,
        109,110,111 };

    public static final int[] BANDWIDTH = { 84,85,86,87,88,89,90,91,92 };
    public static final int[] BANDWIDTH_VHF_UHF = { 84,85,86,87 };

    public static final int[] TRANSMIT_OFFSET = { 93,94,95,96,97,98,99,100,101 };
    public static final int TRANSMIT_OFFSET_VHF_UHF_SIGN = 88;
    public static final int[] TRANSMIT_OFFSET_VHF_UHF = { 89,90,91,92,93,94,
        95,96,97,98,99,100,101 };
    
    public static final int[] BASE_FREQUENCY = { 112,113,114,115,116,117,118,
        119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,
        137,138,139,140,141,142,143 };
    
    public IdentifierUpdate( BitSetBuffer message, 
                             DataUnitID duid,
                             AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }
    
    @Override
    public String getEventType()
    {
    	return getOpcode().getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        sb.append( " CHAN:" + getIdentifier() );

        sb.append( " DOWNLINK:" + getDownlinkFrequency() );
        
        sb.append( " UPLINK:" + ( getUplinkFrequency() ) );
        
        sb.append( " BW:" + getBandwidth() );
        
        sb.append( " SPACING:" + getChannelSpacing() );
        
        return sb.toString();
    }

    public int getIdentifier()
    {
        return mMessage.getInt( CHANNEL_IDENTIFIER );
    }
    
    /**
     * Channel spacing in hertz
     */
    public int getChannelSpacing()
    {
        return mMessage.getInt( CHANNEL_SPACING ) * 125; //Hertz
    }

    /**
     * Base frequency in hertz
     */
    public long getBaseFrequency()
    {
        return mMessage.getLong( BASE_FREQUENCY ) * 5l; //Hertz
    }
    
    public boolean isVHF_UHF()
    {
        long base = getBaseFrequency();
        
        return ( ( 136000000 <= base && base <= 172000000 ) ||
                 ( 380000000 <= base && base <= 512000000 ) );
    }

    /**
     * Channel bandwidth in hertz
     */
    public int getBandwidth()
    {
        int bandwidth = mMessage.getInt( BANDWIDTH );

        if( isVHF_UHF() )
        {
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
        else
        {
            return bandwidth * 125;
        }
    }

    /**
     * Transmit offset in hertz
     */
    public long getOffset()
    {
        if( isVHF_UHF() )
        {
            long offset = mMessage.getLong( TRANSMIT_OFFSET_VHF_UHF );
            
            if( mMessage.get( TRANSMIT_OFFSET_VHF_UHF_SIGN ) )
            {
                return offset;
            }
            else
            {
                return -offset;
            }
        }
        else
        {
            return mMessage.getLong( TRANSMIT_OFFSET ) * 250l;
        }
    }
    
    /**
     * Uplink ( radio to tower) frequency in hertz
     */
    public long getUplinkFrequency()
    {
        return getBaseFrequency() - getOffset();
    }

    /**
     * Downlink ( tower to radio ) frequency in hertz
     */
    public long getDownlinkFrequency()
    {
        return getBaseFrequency();
    }
}
