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

package com.github.dsheirer.sdrplay.parameter.event;

import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_h;

/**
 * (Callback) Event Type (sdrplay_api_EventT)
 */
public enum EventType
{
    GAIN_CHANGE(sdrplay_api_h.sdrplay_api_GainChange(), "GAIN CHANGE"),
    POWER_OVERLOAD_CHANGE(sdrplay_api_h.sdrplay_api_PowerOverloadChange(), "POWER OVERLOAD CHANGE"),
    DEVICE_REMOVED(sdrplay_api_h.sdrplay_api_DeviceRemoved(), "DEVICE REMOVED"),
    RSP_DUO_MODE_CHANGE(sdrplay_api_h.sdrplay_api_RspDuoModeChange(), "RSP-DUO MODE CHANGE"),
    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mDescription;

    EventType(int value, String description)
    {
        mValue = value;
        mDescription = description;
    }

    /**
     * Numeric value
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Lookup the entry from a return code
     * @param value to lookup
     * @return entry or UNKNOWN if the code is not recognized
     */
    public static EventType fromValue(int value)
    {
        for(EventType status: EventType.values())
        {
            if(status.getValue() == value)
            {
                return status;
            }
        }
        
        return UNKNOWN;
    }

    @Override
    public String toString()
    {
        return mDescription;
    }
}
