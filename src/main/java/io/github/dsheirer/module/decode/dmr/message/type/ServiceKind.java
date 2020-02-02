package io.github.dsheirer.module.decode.dmr.message.type;

/**
 * Service Kind enumeration for Tier III Ahoy Messages
 *
 * See: TS 102 361.4 paragraph 7.2.12
 */
public enum ServiceKind
{
    INDIVIDUAL_VOICE_CALL_SERVICE(0, "INDIVIDUAL VOICE CALL SERVICE"),
    TALKGROUP_VOICE_CALL_SERVICE(1, "TALKGROUP VOICE CALL SERVICE"),
    INDIVIDUAL_PACKET_CALL_SERVICE(2, "INDIVIDUAL PACKET CALL SERVICE"),
    TALKGROUP_PACKET_CALL_SERVICE(3, "TALKGROUP PACKET CALL SERVICE"),
    INDIVIDUAL_UDT_SHORT_DATA_CALL_SERVICE(4, "INDIVIDUAL UDT SHORT DATA CALL SERVICE"),
    TALKGROUP_UDT_SHORT_DATA_CALL_SERVICE(5, "TALKGROUP UDT SHORT DATA CALL SERVICE"),
    UDT_SHORT_DATA_POLLING_SERVICE(6, "UDT SHORT DATA POLLING SERVICE"),
    STATUS_TRANSPORT_SERVICE(7, "STATUS TRANSPORT SERVICE"),
    CALL_DIVERSION_SERVICE(8, "CALL DIVERSION SERVICE"),
    CALL_ANSWER_SERVICE(9, "CALL ANSWER SERVICE"),
    FULL_DUPLEX_MS_TO_MS_VOICE_CALL_SERVICE(10, "FULL DUPLEX MS TO MS VOICE CALL SERVICE"),
    FULL_DUPLEX_MS_TO_MS_PACKET_CALL_SERVICE(11, "FULL DUPLEX MS TO MS PACKET CALL SERVICE"),
    RESERVED_12(12, "RESERVED 12"),
    SUPPLEMENTARY_SERVICE(13, "SUPPLEMENTARY SERVICE"),
    AUTHENTICATE_REGISTER_RADIO_CHECK_SERVICE(14, "REGISTRATION OR RADIO CHECK SERVICE"),
    CANCEL_CALL_SERVICE(15, "CANCEL CALL SERVICE"),
    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    /**
     * Constructs an instance
     * @param value of the entry
     * @param label for the entry
     */
    ServiceKind(int value, String label)
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
     * @param value of the service type
     * @return type or UNKNOWN
     */
    public static ServiceKind fromValue(int value)
    {
        if(0 <= value && value <= 15)
        {
            return ServiceKind.values()[value];
        }

        return UNKNOWN;
    }
}
