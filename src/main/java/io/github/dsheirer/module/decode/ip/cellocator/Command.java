/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.module.decode.ip.cellocator;

import java.util.Map;
import java.util.TreeMap;

/**
 * Cellocator - Commands Enumeration
 */
public enum Command
{
    IMMEDIATE_STATUS_REQUEST(0x0, "IMMEDIATE STATUS AND LOCATION"),
    UNIT_STATUS_CHANGE(0x2, "CHANGE UNIT STATUS"),
    OUTPUT_STATE_CHANGE(0x3, "CHANGE OUTPUT STATE"),
    DISABLE_ACTIVE_TRANSMISSIONS(0x4, "DISABLE ACTIVE TRANSMISSIONS"),
    TRACKING_CONTROL(0x5, "TRACKING CONTROL"),
    ALARM_CADENCE_CONTROL(0x6, "ALARM CADENCE CONTROL"),
    COMMENCE_GRADUAL_ENGINE_STOP(0x7, "COMMENCE GRADUAL ENGINE STOP"),
    INITIATE_CSD_SESSION(0xC, "INITIATE CSD SESSION"),
    ERASE_TRACKING_LOG(0xD, "ERASE TRACKING LOG"),
    RESET_GPS_RECEIVER(0xE, "RESET GPS RECEIVER"),
    SEQUENCE_DETECTION_LEARN(0xF, "SEQUENCE DETECTION LEARN"),
    FORCE_GPS_ENERGIZING(0x10, "FORCE GPS ACTIVATION"),
    CONNECT_TO_SERVER(0x12, "CONNECT TO SERVER"),
    MANUFACTURER_RESERVED(0x13, "MANUFACTURER RESERVED"),
    CALIBRATE_FREQUENCY_COUNTERS(0x14, "CALIBRATE FREQUENCY COUNTERS"),
    CONTROL_COM_PORT_TRANSPARENT_MODE(0x15, "CONTROL COM PORT TRANSPARENT MODE"),
    QUERY_CONNECTED_TRAILER_ID(0x16, "QUERY CONNECTED TRAILER ID"),
    CAMERA_SUPPORT(0x18, "CAMERA SUPPORT"),
    NANO_WAKEUP_AND_STATUS_REQUEST(0x19, "NANO WAKEUP AND STATUS REQUEST"),
    ENABLE_MODEM_FOTA_SESSION(0x1A, "ENABLE MODEM FOTA SESSION"),
    PHSN_CONTROL(0x1B, "PHSN CONTROL"),
    CALL_CONTROL(0x1C, "CALL SHARING 2 CONTROL"),
    CELLO_TRACK_T(0x1D, "CELLO TRACK T"),
    CALIBRATE_CAN_GPS_SPEED(0x1F, "CALIBRATE CAN GPS SPEED"),

    UNKNOWN(-1, "UNKNOWN COMMAND");

    private int mValue;
    private String mLabel;

    /**
     * Constructs an instance
     * @param value of the command
     * @param label for pretty printing
     */
    Command(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Value of the command
     */
    public int getValue()
    {
        return mValue;
    }

    private static Map<Integer,Command> LOOKUP_MAP = new TreeMap<>();

    static
    {
        for(Command command: Command.values())
        {
            LOOKUP_MAP.put(command.getValue(), command);
        }
    }

    /**
     * Lookup a command from its value
     * @param value of command to lookup
     * @return command or UNKNOWN
     */
    public static Command fromValue(int value)
    {
        if(LOOKUP_MAP.containsKey(value))
        {
            return LOOKUP_MAP.get(value);
        }

        return UNKNOWN;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
