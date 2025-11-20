package io.github.dsheirer.module.decode.nxdn.type;

/**
 * Data call options
 */
public class DataCallOption extends CallOption
{
    /**
     * Constructs an instance
     *
     * @param value for the field
     */
    public DataCallOption(int value)
    {
        super(value);
    }

    @Override
    public String toString()
    {
        return getTransmissionMode() + " " + getDuplex();
    }
}
