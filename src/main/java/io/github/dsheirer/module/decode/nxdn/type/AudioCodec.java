package io.github.dsheirer.module.decode.nxdn.type;

/**
 * AMBE+ audio code enumeration
 */
public enum AudioCodec
{
    HALF_RATE("AMBE+ HALF-RATE"),
    FULL_RATE("AMBE+ FULL-RATE");

    private String mLabel;

    AudioCodec(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
