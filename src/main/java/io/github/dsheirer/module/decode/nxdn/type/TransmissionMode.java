package io.github.dsheirer.module.decode.nxdn.type;

/**
 * Transmission rate and voice vocoder.
 */
public enum TransmissionMode
{
    RATE_4800("RATE:4800"),
    RATE_9600("RATE:9600");

    private final String mLabel;

    /**
     * Constructs an instance
     *
     * @param label to display
     */
    TransmissionMode(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
