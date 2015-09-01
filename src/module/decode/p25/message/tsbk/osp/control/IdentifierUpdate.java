package module.decode.p25.message.tsbk.osp.control;

import module.decode.p25.message.IBandIdentifier;
import module.decode.p25.message.tsbk.TSBKMessage;
import module.decode.p25.reference.DataUnitID;
import alias.AliasList;
import bits.BinaryMessage;

public abstract class IdentifierUpdate extends TSBKMessage 
									   implements IBandIdentifier
{
    public static final int[] IDENTIFIER = { 80,81,82,83 };

    public static final int[] CHANNEL_SPACING = { 102,103,104,105,106,107,108,
    	109,110,111 };
    
    public static final int[] BASE_FREQUENCY = { 112,113,114,115,116,117,118,
        119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,
        137,138,139,140,141,142,143 };
    
    public IdentifierUpdate( BinaryMessage message, 
                             DataUnitID duid,
                             AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }
    
    @Override
    public int getIdentifier()
    {
        return mMessage.getInt( IDENTIFIER );
    }
    
    @Override
    public long getChannelSpacing()
    {
    	return mMessage.getLong( CHANNEL_SPACING ) * 125l;
    }

    @Override
    public long getBaseFrequency()
    {
        return mMessage.getLong( BASE_FREQUENCY ) * 5l;
    }
    
    @Override
    public abstract long getTransmitOffset();
}
