package io.github.dsheirer.module.decode.dmr.message;

public enum LCSS
{
    /**
     * Link Control(LC) Single Fragment -or- CSBK Signalling First Fragment
     */
    SINGLE_FRAGMENT("[FL]"),

    /**
     * LC First Fragment
     */
    FIRST_FRAGMENT("[F-]"),

    /**
     * LC or CSBK Last Fragment
     */
    LAST_FRAGMENT("[-L]"),

    /**
     * LC or CSBK Continuation Fragment
     */
    CONTINUATION_FRAGMENT("[--]"),

    /**
     * Unknown Fragment
     */
    UNKNOWN("[**]");

    private String mLabel;

    LCSS(String label)
    {
        mLabel = label;
    }

    public static LCSS fromValue(int value)
    {
        if(0 <= value && value <= 4)
        {
            return LCSS.values()[value];
        }

        return UNKNOWN;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}