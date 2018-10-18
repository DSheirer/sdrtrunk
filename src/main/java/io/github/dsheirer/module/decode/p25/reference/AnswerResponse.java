package io.github.dsheirer.module.decode.p25.reference;

public enum AnswerResponse
{
    PROCEED,
    DENY,
    WAIT,
    UNKNOWN;

    public static AnswerResponse fromValue(int value)
    {
        switch(value)
        {
            case 0x20:
                return PROCEED;
            case 0x21:
                return DENY;
            case 0x22:
                return WAIT;
            default:
                return UNKNOWN;
        }
    }
}
