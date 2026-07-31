/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

/**
 * Cause values for disconnect (DISC).
 */
public enum CauseDISC
{
    USER_DISCONNECT(0x10, "USER DISCONNECT"),
    PSTN_DISCONNECT(0x11, "PSTN DISCONNECT"),
    TIMER_DISCONNECT(0x14, "TIMER DISCONNECT"),
    OTHER_DISCONNECT(0x1F, "OTHER DISCONNECT"),
    TC_TIMER_DISCONNECT(0x20, "TRUNK CONTROL TIMER DISCONNECT"),
    OTHER_TC_DISCONNECT(0x2F, "TRUNK CONTROL DISCONNECT"),
    CHANNEL_UNAVAILABLE(0x50, "CHANNEL UNAVAILABLE"),
    NETWORK_FAILURE(0x51, "NETWORK FAILURE"),
    TEMPORARY_FAILURE(0x52, "TEMPORARY FAILURE"),
    EQUIPMENT_CONGESTION(0x53, "EQUIPMENT CONGESTION"),
    RESOURCE_UNAVAILABLE(0x5F, "RESOURCE UNAVAILABLE"),
    SERVICE_UNAVAILABLE(0x60, "SERVICE UNAVAILABLE"),
    SERVICE_UNSUPPORTED(0x61, "SERVICE UNSUPPORTED"),
    SERVICE_UNAVAILABLE_OR_UNSUPPORTED(0x6F, "SERVICE UNAVAILABLE OR UNSUPPORTED"),
    MISSING_MANDATORY_INFORMATION_ELEMENTS(0x70, "MISSING MANDATORY INFORMATION ELEMENTS"),
    UNDEFINED_INFORMATION_ELEMENT_OR_INVALID_CONTENTS(0x71, "UNDEFINED INFORMATION ELEMENTS OR INVALID_CONTENTS"),
    PROCEDURE_ERROR(0x7F, "PROCEDURE ERROR"),
    UNKNOWN(-1, "UNKNOWN");

    private final int mValue;
    private final String mLabel;

    /**
     * Constructs an instance
     *
     * @param value for the entry
     * @param label to display
     */
    CauseDISC(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Transmitted value.
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Display label.
     */
    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Utility method to lookup the matching entry.
     *
     * @param value to look up
     * @return matching entry or UNKNOWN
     */
    public static CauseDISC fromValue(int value)
    {
        for(CauseDISC cause : CauseDISC.values())
        {
            if(cause.getValue() == value)
            {
                return cause;
            }
        }

        return UNKNOWN;
    }
}
