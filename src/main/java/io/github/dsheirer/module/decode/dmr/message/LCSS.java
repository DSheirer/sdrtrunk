package io.github.dsheirer.module.decode.dmr.message;

public enum LCSS
{
    /**
     * Link Control(LC) Single Fragment -or- CSBK Signalling First Fragment
     */
    SINGLE_FRAGMENT,

    /**
     * LC First Fragment
     */
    FIRST_FRAGMENT,

    /**
     * LC or CSBK Last Fragment
     */
    LAST_FRAGMENT,

    /**
     * LC or CSBK Continuation Fragment
     */
    CONTINUATION_FRAGMENT,

    /**
     * Unknown Fragment
     */
    UNKNOWN;

    public static LCSS fromValue(int value)
    {
        if(0 <= value && value <= 4)
        {
            return LCSS.values()[value];
        }

        return UNKNOWN;
    }
}