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

package io.github.dsheirer.source.tuner.sdrplay.api.error;

import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_h;

/**
 * Debug Level
 */
public enum DebugLevel
{
    DISABLE(sdrplay_api_h.sdrplay_api_DbgLvl_Disable(), "DISABLE"),
    VERBOSE(sdrplay_api_h.sdrplay_api_DbgLvl_Verbose(), "VERBOSE"),
    WARNING(sdrplay_api_h.sdrplay_api_DbgLvl_Warning(), "WARNING"),
    ERROR(sdrplay_api_h.sdrplay_api_DbgLvl_Error(), "ERROR"),
    MESSAGE(sdrplay_api_h.sdrplay_api_DbgLvl_Message(), "MESSAGE"),
    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mDescription;

    DebugLevel(int value, String description)
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
    public static DebugLevel fromValue(int value)
    {
        for(DebugLevel status: DebugLevel.values())
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
