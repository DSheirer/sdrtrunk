package io.github.dsheirer.source.tuner.airspy.hf;

/**
 * Airspy HF models
 */
public enum BoardId
{
    UNKNOWN(0, "UNKNOWN"),
    HF_REV_A(1, "HF+"),
    HF_DISCOVERY_REV_A(2, "HF Discovery"),
    INVALID(0xFF, "INVALID");

    private int mValue;
    private String mLabel;

    /**
     * Constructs an instance
     * @param value of the entry
     * @param label for display
     */
    BoardId(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Lookup a board ID from a value
     * @param value to lookup
     * @return matching entry or INVALID
     */
    public static BoardId fromValue(int value)
    {
        return switch(value)
        {
            case 0 -> UNKNOWN;
            case 1 -> HF_REV_A;
            case 2 -> HF_DISCOVERY_REV_A;
            default -> INVALID;
        };
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
