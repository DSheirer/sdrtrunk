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

package io.github.dsheirer.source.tuner.sdrplay.api.device;

import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_h;

/**
 * RSP-Duo Mode
 */
public enum RspDuoMode
{
    SINGLE_TUNER(sdrplay_api_h.sdrplay_api_RspDuoMode_Single_Tuner(), "SINGLE TUNER"),
    DUAL_TUNER(sdrplay_api_h.sdrplay_api_RspDuoMode_Dual_Tuner(), "DUAL TUNER"),
    MASTER(sdrplay_api_h.sdrplay_api_RspDuoMode_Master(), "MASTER"),
    SLAVE(sdrplay_api_h.sdrplay_api_RspDuoMode_Slave(), "SLAVE"),
    UNKNOWN(sdrplay_api_h.sdrplay_api_RspDuoMode_Unknown(), "UNKNOWN");

    private int mValue;
    private String mDescription;

    RspDuoMode(int value, String description)
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
     * @return entry or UKNOWN if the code is not recognized
     */
    public static RspDuoMode fromValue(int value)
    {
        for(RspDuoMode status: RspDuoMode.values())
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
