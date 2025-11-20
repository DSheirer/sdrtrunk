package io.github.dsheirer.module.decode.nxdn.layer3.call;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;

/**
 * Voice call reception request.
 */
public class VoiceCallReceptionRequest extends VoiceCall
{
    /**
     * Constructs an instance
     *
     * @param message   with binary data
     * @param timestamp for the message
     * @param type      of message
     */
    public VoiceCallReceptionRequest(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type)
    {
        super(message, timestamp, type);
    }
}
