package io.github.dsheirer.module.decode.nxdn.type;

/**
 * Call options field base class.
 */
public abstract class CallOption
{
    private static int MASK_DUPLEX = 0x10;
    private static int MASK_TRANSMISSION_MODE = 0x02;
    protected final int mValue;

    /**
     * Constructs an instance
     * @param value for the field
     */
    public CallOption(int value)
    {
        mValue = value;
    }

    /**
     * Indicates the duplex mode for the call.
     * @return duplex mode.
     */
    public Duplex getDuplex()
    {
        return (mValue & MASK_DUPLEX) == MASK_DUPLEX ? Duplex.DUPLEX : Duplex.HALF_DUPLEX;
    }

    /**
     * Transmission mode for the call.
     * @return mode
     */
    public TransmissionMode getTransmissionMode()
    {
        return (mValue & MASK_TRANSMISSION_MODE) == MASK_TRANSMISSION_MODE ?
                TransmissionMode.RATE_9600 : TransmissionMode.RATE_4800;
    }
}
