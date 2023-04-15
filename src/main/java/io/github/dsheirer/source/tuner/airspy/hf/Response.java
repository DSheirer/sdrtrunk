package io.github.dsheirer.source.tuner.airspy.hf;

/**
 * Responses to Airspy HF requests
 */
public enum Response
{
    SUCCESS(0),
    ERROR(-1),
    UNSUPPORTED(-2);

    private int mValue;

    Response(int value)
    {
        mValue = value;
    }

    /**
     * Lookup the enumeration entry from the value.
     * @param value to lookup
     * @return entry or UNSUPPORTED if there are no matches.
     */
    public static Response fromValue(int value)
    {
        return switch(value)
        {
            case 0 -> SUCCESS;
            case -1 -> ERROR;
            default -> UNSUPPORTED;
        };
    }
}
