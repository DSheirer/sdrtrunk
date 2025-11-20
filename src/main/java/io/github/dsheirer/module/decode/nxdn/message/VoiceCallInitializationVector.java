package io.github.dsheirer.module.decode.nxdn.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.LongField;
import io.github.dsheirer.identifier.Identifier;

import java.util.Collections;
import java.util.List;

/**
 * Voice call encryption initialization vector.
 */
public class VoiceCallInitializationVector extends NXDNMessage
{
    private static final LongField INITIALIZATION_VECTOR = LongField.length64(OCTET_1);

    /**
     * Constructs an instance
     *
     * @param message   with binary data
     * @param timestamp for the message
     */
    public VoiceCallInitializationVector(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageType());
        sb.append(" IV:").append(getInitializationVector());
        return sb.toString();
    }

    /**
     * Initialization vector as hex string.
     */
    public String getInitializationVector()
    {
        return Long.toHexString(getMessage().getLong(INITIALIZATION_VECTOR)).toUpperCase();
    }

    @Override
    public NXDNMessageType getMessageType()
    {
        return NXDNMessageType.TC_VOICE_CALL_INITIALIZATION_VECTOR;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
