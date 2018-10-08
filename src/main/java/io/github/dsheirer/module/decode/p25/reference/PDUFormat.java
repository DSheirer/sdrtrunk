package io.github.dsheirer.module.decode.p25.reference;

public enum PDUFormat
{
    F00("F00", 0),
    F01("F01", 1),
    F02("F02", 2),
    RESPONSE_PACKET_HEADER_FORMAT("RESPONSE", 3),
    F04("F04", 4),
    F05("F05", 5),
    F06("F06", 6),
    F07("F07", 7),
    F08("F08", 8),
    F09("F09", 9),
    F10("F10", 10),
    F11("F11", 11),
    F12("F12", 12),
    F13("F13", 13),
    F14("F14", 14),
    F15("F15", 15),
    F16("F16", 16),
    F17("F17", 17),
    F18("F18", 18),
    F19("F19", 19),
    F20("F20", 20),
    UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL("UNCONFIRMED MBTC", 21),
    PACKET_DATA("PACKET DATA", 22),
    ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL("ALTERNATE MBTC", 23),
    F24("F24", 24),
    F25("F25", 25),
    F26("F26", 26),
    F27("F27", 27),
    F28("F28", 28),
    F29("F29", 29),
    F30("F30", 30),
    F31("F31", 31),
    UNKNOWN("UNKN", -1);

    private String mLabel;
    private int mValue;

    PDUFormat(String label, int value)
    {
        mLabel = label;
        mValue = value;
    }

    public String getLabel()
    {
        return mLabel;
    }

    public int getValue()
    {
        return mValue;
    }

    public static PDUFormat fromValue(int value)
    {
        if(0 <= value && value <= 31)
        {
            return values()[value];
        }

        return UNKNOWN;
    }

    public String toString()
    {
        return mLabel;
    }
}
