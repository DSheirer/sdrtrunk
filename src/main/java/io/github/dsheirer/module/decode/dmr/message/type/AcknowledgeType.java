package io.github.dsheirer.module.decode.dmr.message.type;

/**
 * Acknowledge message type enumeration
 */
public enum AcknowledgeType
{
    ACKNOWLEDGE(0, "ACKNOWLEDGED"),
    NOT_ACKNOWLEDGE(1, "REJECTED/REFUSED"),
    QUEUED(2, "QUEUED"),
    WAIT(3, "WAIT"),
    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    /**
     * Constructs an instance
     * @param value of the entry
     * @param label for the entry
     */
    AcknowledgeType(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Utility method to lookup the type from a value.
     * @param value of the acknowledge type
     * @return type or UNKNOWN
     */
    public static AcknowledgeType fromValue(int value)
    {
        if(0 <= value && value <= 3)
        {
            return AcknowledgeType.values()[value];
        }

        return UNKNOWN;
    }
}
