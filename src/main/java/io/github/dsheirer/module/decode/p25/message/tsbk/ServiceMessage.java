package io.github.dsheirer.module.decode.p25.message.tsbk;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.message.P25Message;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.alias.AliasList;

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
    
    public P25Message.DuplexMode getDuplexMode()
    {
        return mMessage.get( DUPLEX_MODE ) ? P25Message.DuplexMode.FULL : P25Message.DuplexMode.HALF;
    }

    public P25Message.SessionMode getSessionMode()
    {
        return mMessage.get( SESSION_MODE ) ? 
                P25Message.SessionMode.CIRCUIT : P25Message.SessionMode.PACKET;
    }
}
