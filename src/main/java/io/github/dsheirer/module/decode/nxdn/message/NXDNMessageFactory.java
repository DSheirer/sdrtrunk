package io.github.dsheirer.module.decode.nxdn.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Factory for creating NXDN messages
 */
public class NXDNMessageFactory
{
    public static NXDNMessage getTCOutbound(NXDNMessageType type, CorrectedBinaryMessage message, long timestamp)
    {
        return switch (type)
        {
            case TC_VOICE_CALL -> new VoiceCall(message, timestamp);
            case TC_VOICE_CALL_INITIALIZATION_VECTOR -> new VoiceCallInitializationVector(message, timestamp);
            case TC_DATA_CALL_HEADER -> new DataCallHeader(message, timestamp);
            case TC_DATA_CALL_BLOCK -> new DataCallBlock(message, timestamp);
            default -> null;
        };
    }
}
