package decode.p25.message.tsbk;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.reference.DataUnitID;

public abstract class ChannelGrant extends ServiceMessage
{
    public static final int[] PRIORITY = { 85,86,87 };
    public static final int[] CHANNEL_ID = { 88,89,90,91 };
    public static final int[] CHANNEL_NUMBER = { 92,93,94,95,96,97,98,99,100,
        101,102,103 };
    
    public ChannelGrant( BitSetBuffer message, 
    					 DataUnitID duid,
    					 AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    /**
     * 1 = Lowest, 4 = Default, 7 = Highest
     */
    public int getPriority()
    {
        return mMessage.getInt( PRIORITY );
    }
    
    public int getChannelID()
    {
        return mMessage.getInt( CHANNEL_ID );
    }
    
    public int getChannelNumber()
    {
        return mMessage.getInt( CHANNEL_NUMBER );
    }
}
