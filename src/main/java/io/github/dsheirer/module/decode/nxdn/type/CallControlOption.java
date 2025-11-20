package io.github.dsheirer.module.decode.nxdn.type;

/**
 * Call control options field.
 */
public class CallControlOption
{
    private static int MASK_EMERGENCY = 0x80;
    private static int MASK_LOCATION_ID = 0x40;
    private static int MASK_PRIORITY_PAGING = 0x20;
    private static int MASK_SUPPLEMENTARY_PROCEDURES = 0x07;

    private final int mValue;

    /**
     * Constructs an instance of the CC option field.
     * @param value from the field.
     */
    public CallControlOption(int value)
    {
        mValue = value;
    }

    /**
     * Emergency indicator.
     * @return true if emergency message or false if normal message.
     */
    public boolean isEmergency()
    {
        return (mValue & MASK_EMERGENCY) == MASK_EMERGENCY;
    }

    /**
     * Indicates if the message has a location ID field included.  Incoming call on CC by a visiting SU will
     * include the location ID in the message.
     * @return location ID included in the message
     */
    public boolean hasLocationId()
    {
        return (mValue & MASK_LOCATION_ID) == MASK_LOCATION_ID;
    }

    /**
     * Indicates if this is priority or normal paging.
     */
    public boolean isPriorityPaging()
    {
        return (mValue & MASK_PRIORITY_PAGING) == MASK_PRIORITY_PAGING;
    }

    /**
     * Value for supplementary procedures.
     * @return value.
     */
    public int getMaskSupplementaryProceduresValue()
    {
        return mValue & MASK_SUPPLEMENTARY_PROCEDURES;
    }
}
