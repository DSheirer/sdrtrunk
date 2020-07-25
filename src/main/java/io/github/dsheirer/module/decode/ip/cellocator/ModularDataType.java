package io.github.dsheirer.module.decode.ip.cellocator;

/**
 * Modular message data types
 */
public enum ModularDataType
{
    FIRMWARE_PLATFORM_MANIFEST(1),
    CAN_DATA(2),
    CAN_TRIGGER_DATA(3),
    TIME_AND_LOCATION_DATA(4),
    ACCELEROMETER_DATA(5),
    PSP_ALARM_SYSTEM_DATA(6),
    USAGE_COUNTER_DATA(7),
    COMMAND_AUTHENTICATION_TABLE_DATA(8),
    GSM_NEIGHBOR_LIST(9),
    MAINTENANCE_SERVER_PLATFORM_MANIFEST(10),
    UNKNOWN(0);

    private int mValue;

    ModularDataType(int value)
    {
        mValue = value;
    }

    public int getValue()
    {
        return mValue;
    }

    public static ModularDataType fromValue(int value)
    {
        for(ModularDataType type: ModularDataType.values())
        {
            if(type.getValue() == value)
            {
                return type;
            }
        }

        return UNKNOWN;
    }
}
