package io.github.dsheirer.module.decode.p25.reference;

public enum CancelReason
{
    NO_REASON("NO REASON"),
    TERMINATE_QUEUED_CONDITION("TERMINATE QUEUED CONDITION"),
    TERMINATE_RESOURCE_ASSIGNMENT("TERMINATE RESOURCE ASSIGNMENT"),
    RESERVED("RESERVED"),
    USER_OR_SYSTEM_DEFINED("USER OR SYSTEM DEFINED"),
    UNKNOWN("UNKNOWN");

    private String mLabel;

    CancelReason(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    public static CancelReason fromCode(int code)
    {
        if(code == 0x00)
        {
            return CancelReason.NO_REASON;
        }
        else if(code == 0x10)
        {
            return TERMINATE_QUEUED_CONDITION;
        }
        else if(code == 0x20)
        {
            return TERMINATE_RESOURCE_ASSIGNMENT;
        }
        else if(code < 0x7F)
        {
            return RESERVED;
        }

        return USER_OR_SYSTEM_DEFINED;
    }
}
