package io.github.dsheirer.module.decode.nxdn.type;

public enum CallType
{
    GROUP_BROADCAST(0, "GROUP BROADCAST"),
    GROUP_CONFERENCE(1, "GROUP CONFERENCE"),
    UNSPECIFIED(2, "UNSPECIFIED"),
    RESERVED_3(3, "RESERVED 3"),
    INDIVIDUAL(4, "INDIVIDUAL"),
    RESERVED_5(5, "RESERVED 5"),
    INTERCONNECT(6, "INTERCONNECT"),
    SPEED_DIAL(7, "SPEED DIAL"),
    UNKNOWN(-1, "UNKNOWN");

    private final int mValue;
    private final String mLabel;

    CallType(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    public int getValue()
    {
        return mValue;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Utility method to lookup the call type from the transmitted value.
     *
     * @param value to lookup
     * @return matching entry or UNKNOWN
     */
    public static CallType fromValue(int value)
    {
        return switch(value)
        {
            case 0 -> GROUP_BROADCAST;
            case 1 -> GROUP_CONFERENCE;
            case 2 -> UNSPECIFIED;
            case 3 -> RESERVED_3;
            case 4 -> INDIVIDUAL;
            case 5 -> RESERVED_5;
            case 6 -> INTERCONNECT;
            case 7 -> SPEED_DIAL;
            default -> UNKNOWN;
        };
    }
}
