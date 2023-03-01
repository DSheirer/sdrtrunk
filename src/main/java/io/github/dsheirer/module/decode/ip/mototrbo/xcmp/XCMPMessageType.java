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

package io.github.dsheirer.module.decode.ip.mototrbo.xcmp;

import java.util.Map;
import java.util.TreeMap;

/**
 * XCMP Message Types enumeration
 */
public enum XCMPMessageType
{
    NETWORK_FREQUENCY_FILE(2, "NETWORK FREQUENCY FILE"),
    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    XCMPMessageType(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Numeric value for the type
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

    private static final Map<Integer,XCMPMessageType> LOOKUP_MAP = new TreeMap<>();

    static
    {
        for(XCMPMessageType type: XCMPMessageType.values())
        {
            LOOKUP_MAP.put(type.getValue(), type);
        }
    }

    /**
     * Lookup the XCMP Message type from the value
     */
    public static XCMPMessageType fromValue(int value)
    {
        if(LOOKUP_MAP.containsKey(value))
        {
            return LOOKUP_MAP.get(value);
        }

        return UNKNOWN;
    }
}
