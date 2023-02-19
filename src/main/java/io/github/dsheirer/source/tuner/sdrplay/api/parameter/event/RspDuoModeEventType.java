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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.event;

import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_h;

/**
 * RSP-Duo Mode Callback Event type
 */
public enum RspDuoModeEventType
{
    MASTER_INITIALIZED(sdrplay_api_h.sdrplay_api_MasterInitialised(), "MASTER INITIALIZED"),
    SLAVE_ATTACHED(sdrplay_api_h.sdrplay_api_SlaveAttached(), "SLAVE ATTACHED"),
    SLAVE_DETACHED(sdrplay_api_h.sdrplay_api_SlaveDetached(), "SLAVE DETACHED"),
    SLAVE_INITIALIZED(sdrplay_api_h.sdrplay_api_SlaveInitialised(), "SLAVE INITIALIZED"),
    SLAVE_UNINITIALIZED(sdrplay_api_h.sdrplay_api_SlaveUninitialised(), "SLAVE UNINITIALIZED"),
    MASTER_DLL_DISAPPEARED(sdrplay_api_h.sdrplay_api_MasterDllDisappeared(), "MASTER DLL DISAPPEARED"),
    SLAVE_DLL_DISAPPEARED(sdrplay_api_h.sdrplay_api_SlaveDllDisappeared(), "SLAVE DLL DISAPPEARED"),
    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mDescription;

    RspDuoModeEventType(int value, String description)
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
    public static RspDuoModeEventType fromValue(int value)
    {
        for(RspDuoModeEventType status: RspDuoModeEventType.values())
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
