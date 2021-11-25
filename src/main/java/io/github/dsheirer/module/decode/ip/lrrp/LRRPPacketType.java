/*
 * *****************************************************************************
 * Copyright (C) 2014-2021 Dennis Sheirer
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

package io.github.dsheirer.module.decode.ip.lrrp;

/**
 * LRRP Packet Type enumeration
 */
public enum LRRPPacketType
{
    IMMEDIATE_LOCATION_REQUEST(0x05, "IMMEDIATE LOCATION REQUEST"),
    IMMEDIATE_LOCATION_RESPONSE(0x07, "IMMEDIATE LOCATION RESPONSE"),
    TRIGGERED_LOCATION_START_REQUEST(0x09, "TRIGGERED LOCATION START REQUEST"),
    TRIGGERED_LOCATION_START_RESPONSE(0x0B, "TRIGGERED LOCATION START RESPONSE"),
    TRIGGERED_LOCATION(0x0D, "TRIGGERED LOCATION"),
    TRIGGERED_LOCATION_STOP_REQUEST(0x0F, "TRIGGERED LOCATION STOP REQUEST"),
    TRIGGERED_LOCATION_STOP_RESPONSE(0x11, "TRIGGERED LOCATION STOP RESPONSE"),
    PROTOCOL_VERSION_REQUEST(0x14, "PROTOCOL VERSION REQUEST"),
    PROTOCOL_VERSION_RESPONSE(0x15, "PROTOCOL VERSION RESPONSE"),

    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    LRRPPacketType(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Numeric value of the type.
     */
    public int getValue()
    {
        return mValue;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Lookup the LRRP packet type from the value.
     * @param value to lookup
     * @return entry or UNKNOWN
     */
    public static LRRPPacketType fromValue(int value)
    {
        for(LRRPPacketType lrrpPacketType: LRRPPacketType.values())
        {
            if(lrrpPacketType.getValue() == value)
            {
                return lrrpPacketType;
            }
        }

        return UNKNOWN;
    }
}
