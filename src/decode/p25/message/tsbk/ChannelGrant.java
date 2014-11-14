package decode.p25.message.tsbk;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.reference.DataUnitID;

public abstract class ChannelGrant extends TSBKMessage
{
    /* Service Options */
    public static final int EMERGENCY_FLAG = 80;
    public static final int ENCRYPTED_CHANNEL_FLAG = 81;
    public static final int DUPLEX_MODE = 82;
    public static final int SESSION_MODE = 83;
    public static final int[] PRIORITY = { 85,86,87 };
    public static final int[] CHANNEL_ID = { 88,89,90,91 };
    public static final int[] CHANNEL_NUMBER = { 92,93,94,95,96,97,98,99,100,
        101,102,103 };
    
    public enum DuplexMode { HALF, FULL };
    public enum SessionMode { PACKET, CIRCUIT };
    
    public ChannelGrant( BitSetBuffer message, 
                                   DataUnitID duid,
                                   AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    public boolean isEmergency()
    {
        return mMessage.get( EMERGENCY_FLAG );
    }
    
    public boolean isEncryptedChannel()
    {
        return mMessage.get( ENCRYPTED_CHANNEL_FLAG );
    }
    
    public DuplexMode getDuplexMode()
    {
        return mMessage.get( DUPLEX_MODE ) ? DuplexMode.FULL : DuplexMode.HALF;
    }

    public SessionMode getSessionMode()
    {
        return mMessage.get( SESSION_MODE ) ? 
                SessionMode.CIRCUIT : SessionMode.PACKET;
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
