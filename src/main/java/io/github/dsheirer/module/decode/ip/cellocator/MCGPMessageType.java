/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

import java.util.EnumSet;

/**
 * Cellocator MCGP Message Types enumeration with indicated message lengths.
 *
 * Note: message length of -1 indicates a variable length message.
 */
public enum MCGPMessageType
{
    INBOUND_GENERIC_COMMAND(0, 25 * 8, "COMMAND"),
    INBOUND_PROGRAMMING_COMMAND(1, 34 * 8, "PROGRAMMING COMMAND"),
    INBOUND_GENERAL_ACKNOWLEDGE(4, 28 * 8, "ACKNOWLEDGE"),
    INBOUND_FORWARD_DATA_COMMAND(5, -1, "FORWARD DATA COMMAND"),
    INBOUND_MODULAR_REQUEST(9, -1, "MODULAR MESSAGE REQUEST"),

    OUTBOUND_LOCATION_STATUS(0, 70 * 8, "LOCATION AND STATUS"),
    OUTBOUND_PROGRAMMING_STATUS(3, 31 * 8, "CONFIGURATION"),
    OUTBOUND_FORWARDED_LOGGED_DATA_FRAGMENT(7, -1, "FORWARDED LOGGED DATA"),
    OUTBOUND_FORWARDED_REALTIME_DATA(8, -1, "FORWARDED SERIAL_PORT DATA"),
    OUTBOUND_MODULAR_RESPONSE(9, -1, "MODULAR MESSAGE RESPONSE"),
    OUTBOUND_FIRMWARE_UPDATE(10,-1, "FIRMWARE UPDATE"),

    UNKNOWN(-1, -1, "UNKNOWN");

    private int mValue;
    private int mLength;
    private String mLabel;

    MCGPMessageType(int value, int length, String label)
    {
        mValue = value;
        mLength = length;
        mLabel = label;
    }

    public static EnumSet<MCGPMessageType> INBOUND = EnumSet.range(INBOUND_GENERIC_COMMAND, INBOUND_MODULAR_REQUEST);
    public static EnumSet<MCGPMessageType> OUTBOUND = EnumSet.range(OUTBOUND_LOCATION_STATUS, OUTBOUND_FIRMWARE_UPDATE);

    public int getValue()
    {
        return mValue;
    }

    public int getLength()
    {
        return mLength;
    }

    /**
     * Utility method to lookup an entry from a value.
     */
    public static MCGPMessageType fromValue(int value, int messageByteLength)
    {
        switch(value)
        {
            case 0:
                if(messageByteLength == 25)
                {
                    return INBOUND_GENERIC_COMMAND;
                }
                else if(messageByteLength == 70)
                {
                    return OUTBOUND_LOCATION_STATUS;
                }
                break;
            case 1:
                return INBOUND_PROGRAMMING_COMMAND;
            case 3:
                return OUTBOUND_PROGRAMMING_STATUS;
            case 4:
                return INBOUND_GENERAL_ACKNOWLEDGE;
            case 5:
                return INBOUND_FORWARD_DATA_COMMAND;
            case 7:
                return OUTBOUND_FORWARDED_LOGGED_DATA_FRAGMENT;
            case 8:
                return OUTBOUND_FORWARDED_REALTIME_DATA;
            case 9:
                //Defer to the message factory to detect inbound vs outbound message type
                return INBOUND_MODULAR_REQUEST;
            case 10:
                return OUTBOUND_FIRMWARE_UPDATE;
        }

        return UNKNOWN;
    }

    public static MCGPMessageType getOutboundMessageType(int value)
    {
        for(MCGPMessageType type: OUTBOUND)
        {
            if(type.getValue() == value)
            {
                return type;
            }
        }

        return UNKNOWN;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
