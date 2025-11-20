package io.github.dsheirer.module.decode.nxdn.type;

/**
 * Voice call options field.
 */
public class VoiceCallOption extends CallOption
{
    /**
     * Constructs an instance
     *
     * @param value for the field
     */
    public VoiceCallOption(int value)
    {
        super(value);
    }

    public AudioCodec getCodec()
    {
        return (mValue & 0x1) == 0x1 ? AudioCodec.FULL_RATE : AudioCodec.HALF_RATE;
    }

    @Override
    public String toString()
    {
        return getTransmissionMode() + " " + getCodec() + " " + getDuplex();
    }
}
