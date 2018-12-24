package io.github.dsheirer.module.decode.p25.reference;

public enum Status
{
    //Used by subscriber radios for repeated or direct connect communications
    MOBILE_RADIO,

    //Outbound repeater, inbound channel is busy
    REPEATER_BUSY,

    //Subscriber or repeater, unknown status
    UNKNOWN,

    //Outbound repeater, inbound channel is idle
    REPEATER_IDLE;

    public Status fromValue(int value)
    {
        if(0 <= value && value <= 3)
        {
            return values()[value];
        }

        return UNKNOWN;
    }
}
