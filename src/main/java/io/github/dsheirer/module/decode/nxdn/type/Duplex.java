package io.github.dsheirer.module.decode.nxdn.type;

/**
 * Access method
 */
public enum Duplex
{
    HALF_DUPLEX("HALF DUPLEX"),
    DUPLEX("DUPLEX");

    private final String mLabel;

    /**
     * Constructs an instance
     * @param label to display
     */
    Duplex(String label)
    {
        mLabel = label;
    }

    /**
     * Pretty format
     * @return format
     */
    @Override
    public String toString()
    {
        return mLabel;
    }
}
