package io.github.dsheirer.module.decode.p25.reference;

public enum PDUType
{
    /* Outbound */
    OUTBOUND_SNDCP_ACTIVATE_TDS_CONTEXT_ACCEPT("ACTIVATE TDS CONTEXT ACCEPT", 0),
    OUTBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_ACCEPT("DEACTIVATE TDS CONTEXT ACCEPT", 1),
    OUTBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_REQUEST("DEACTIVATE TDS CONTEXT REQUEST", 2),
    OUTBOUND_SNDCP_ACTIVATE_TDS_CONTEXT_REJECT("ACTIVATE TDS CONTEXT REJECT", 3),
    OUTBOUND_SNDCP_RF_UNCONFIRMED_DATA("RF UNCONFIRMED DATA", 4),
    OUTBOUND_SNDCP_RF_CONFIRMED_DATA("RF CONFIRMED DATA", 5),
    OUTBOUND_UNKNOWN("OUTBOUND UNKNOWN PDU TYPE", -1),

    INBOUND_SNDCP_ACTIVATE_TDS_CONTEXT_REQUEST("ACTIVATE TDS CONTEXT REQUEST", 0),
    INBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_ACCEPT("DEACTIVATE TDS CONTEXT ACCEPT", 1),
    INBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_REQUEST("DEACTIVATE TDS CONTEXT REQUEST", 2),
    INBOUND_SNDCP_RF_CONFIRMED_DATA("RF CONFIRMED DATA", 5),
    INBOUND_UNKNOWN("INBOUND UNKNOWN PDU TYPE", -1);

    private String mLabel;
    private int mValue;

    PDUType(String label, int value)
    {
        mLabel = label;
        mValue = value;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    public String getLabel()
    {
        return mLabel;
    }

    public int getValue()
    {
        return mValue;
    }

    public static PDUType fromValue(int value, boolean outbound)
    {
        if(outbound)
        {
            switch(value)
            {
                case 0:
                    return OUTBOUND_SNDCP_ACTIVATE_TDS_CONTEXT_ACCEPT;
                case 1:
                    return OUTBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_ACCEPT;
                case 2:
                    return OUTBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_REQUEST;
                case 3:
                    return OUTBOUND_SNDCP_ACTIVATE_TDS_CONTEXT_REJECT;
                case 4:
                    return OUTBOUND_SNDCP_RF_UNCONFIRMED_DATA;
                case 5:
                    return OUTBOUND_SNDCP_RF_CONFIRMED_DATA;
                default:
                    return OUTBOUND_UNKNOWN;
            }
        }
        else
        {
            switch(value)
            {
                case 0:
                    return INBOUND_SNDCP_ACTIVATE_TDS_CONTEXT_REQUEST;
                case 1:
                    return INBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_ACCEPT;
                case 2:
                    return INBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_REQUEST;
                case 5:
                    return INBOUND_SNDCP_RF_CONFIRMED_DATA;
                default:
                    return INBOUND_UNKNOWN;
            }
        }
    }
}