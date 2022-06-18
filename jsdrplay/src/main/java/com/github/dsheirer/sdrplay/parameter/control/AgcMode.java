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

package com.github.dsheirer.sdrplay.parameter.control;

import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_h;

/**
 * Automatic Gain Control (AGC) Control (mode)
 */
public enum AgcMode
{
    DISABLE(sdrplay_api_h.sdrplay_api_AGC_DISABLE(), "DISABLE"),
    AGC_100_HZ(sdrplay_api_h.sdrplay_api_AGC_100HZ(), "100 Hz"),
    AGC_50_HZ(sdrplay_api_h.sdrplay_api_AGC_50HZ(), "50 Hz"),
    AGC_5_HZ(sdrplay_api_h.sdrplay_api_AGC_5HZ(), "5 Hz"),
    ENABLE(sdrplay_api_h.sdrplay_api_AGC_CTRL_EN(), "ENABLE");

    private int mValue;
    private String mDescription;

    AgcMode(int value, String description)
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
     * @return entry or DISABLE if the code is not recognized
     */
    public static AgcMode fromValue(int value)
    {
        for(AgcMode status: AgcMode.values())
        {
            if(status.getValue() == value)
            {
                return status;
            }
        }
        
        return DISABLE;
    }

    @Override
    public String toString()
    {
        return mDescription;
    }
}
