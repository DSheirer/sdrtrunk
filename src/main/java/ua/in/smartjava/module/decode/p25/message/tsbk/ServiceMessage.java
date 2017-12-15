package ua.in.smartjava.module.decode.p25.message.tsbk;

import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;

public abstract class ServiceMessage extends TSBKMessage
{
    /* Service Options */
    public static final int EMERGENCY_FLAG = 80;
    public static final int ENCRYPTED_CHANNEL_FLAG = 81;
    public static final int DUPLEX_MODE = 82;
    public static final int SESSION_MODE = 83;
    
    public ServiceMessage( BinaryMessage message, 
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
}
