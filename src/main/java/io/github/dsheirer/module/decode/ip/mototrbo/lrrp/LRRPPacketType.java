/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.ip.mototrbo.lrrp;

/**
 * LRRP Packet Type enumeration
 */
public enum LRRPPacketType
{
    IMMEDIATE_LOCATION_REQUEST(0x05, "IMMEDIATE LOCATION REQUEST", false),
    IMMEDIATE_LOCATION_RESPONSE(0x07, "IMMEDIATE LOCATION RESPONSE", true),
    TRIGGERED_LOCATION_START_REQUEST(0x09, "TRIGGERED LOCATION START REQUEST", false),
    TRIGGERED_LOCATION_START_RESPONSE(0x0B, "TRIGGERED LOCATION START RESPONSE", true),
    TRIGGERED_LOCATION(0x0D, "TRIGGERED LOCATION", true),
    TRIGGERED_LOCATION_STOP_REQUEST(0x0F, "TRIGGERED LOCATION STOP REQUEST", false),
    TRIGGERED_LOCATION_STOP_RESPONSE(0x11, "TRIGGERED LOCATION STOP RESPONSE", true),
    PROTOCOL_VERSION_REQUEST(0x14, "PROTOCOL VERSION REQUEST", false),
    PROTOCOL_VERSION_RESPONSE(0x15, "PROTOCOL VERSION RESPONSE", true),

    UNKNOWN(-1, "UNKNOWN", false);

    private int mValue;
    private String mLabel;
    private boolean mIsResponse;

    LRRPPacketType(int value, String label, boolean isResponse)
    {
        mValue = value;
        mLabel = label;
        mIsResponse = isResponse;
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
     * Indicates if this is a response type
     */
    public boolean isResponse()
    {
        return mIsResponse;
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
