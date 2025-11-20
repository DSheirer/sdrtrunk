package io.github.dsheirer.module.decode.nxdn.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.type.VoiceCallOption;

import java.util.List;

/**
 * Voice call message
 */
public class VoiceCall extends Call
{
    private VoiceCallOption mVoiceCallOption;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     */
    public VoiceCall(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public NXDNMessageType getMessageType()
    {
        return NXDNMessageType.TC_VOICE_CALL;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(getCallControlOption().isEmergency())
        {
            sb.append("EMERGENCY ");
        }

        if(getCallControlOption().isPriorityPaging())
        {
            sb.append("PRIORITY PAGING ");
        }

        sb.append(getCallType()).append(" CALL");
        sb.append(" FROM:").append(getSource());
        sb.append(" TO:").append(getDestination());
        sb.append(" ").append(getEncryptionKeyIdentifier());
        sb.append(getCallOption());

        return sb.toString();
    }

    /**
     * Voice call options for the call.
     * @return options
     */
    public VoiceCallOption getCallOption()
    {
        if(mVoiceCallOption == null)
        {
            mVoiceCallOption = new VoiceCallOption(getMessage().getInt(CALL_OPTION));
        }

        return mVoiceCallOption;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of(getSource(), getDestination(), getEncryptionKeyIdentifier());
    }
}
