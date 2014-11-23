package decode.p25.message.tsbk.osp.control;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.reference.DataUnitID;

public abstract class IdentifierUpdate extends TSBKMessage
{
    public static final int[] IDENTIFIER = { 80,81,82,83 };

    public static final int[] CHANNEL_SPACING = { 102,103,104,105,106,107,108,
    	109,110,111 };
    
    public static final int[] BASE_FREQUENCY = { 112,113,114,115,116,117,118,
        119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,
        137,138,139,140,141,142,143 };
    
    public IdentifierUpdate( BitSetBuffer message, 
                             DataUnitID duid,
                             AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }
    
    /**
     * (Band) Identifier
     */
    public int getIdentifier()
    {
        return mMessage.getInt( IDENTIFIER );
    }
    
    /**
     * Channel spacing in hertz
     */
    public long getChannelSpacing()
    {
    	return mMessage.getLong( CHANNEL_SPACING ) * 125l;
    }

    /**
     * Base frequency in hertz
     */
    public long getBaseFrequency()
    {
        return mMessage.getLong( BASE_FREQUENCY ) * 5l;
    }
    
    public abstract long getTransmitOffset();
}
