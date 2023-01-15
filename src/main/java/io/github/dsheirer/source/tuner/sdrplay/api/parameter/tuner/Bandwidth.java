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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner;

import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_h;

/**
 * Bandwidth
 */
public enum Bandwidth
{
    BW_0_200(sdrplay_api_h.sdrplay_api_BW_0_200(), 200_000, "0.200 MHz"),
    BW_0_300(sdrplay_api_h.sdrplay_api_BW_0_300(), 300_000, "0.300 MHz"),
    BW_0_600(sdrplay_api_h.sdrplay_api_BW_0_600(), 600_000, "0.600 MHz"),
    BW_1_536(sdrplay_api_h.sdrplay_api_BW_1_536(), 1_536_000, "1.536 MHz"),
    BW_5_000(sdrplay_api_h.sdrplay_api_BW_5_000(), 5_000_000, "5.000 MHz"),
    BW_6_000(sdrplay_api_h.sdrplay_api_BW_6_000(), 6_000_000, "6.000 MHz"),
    BW_7_000(sdrplay_api_h.sdrplay_api_BW_7_000(), 7_000_000, "7.000 MHz"),
    BW_8_000(sdrplay_api_h.sdrplay_api_BW_8_000(), 8_000_000, "8.000 MHz"),
    UNDEFINED(sdrplay_api_h.sdrplay_api_BW_Undefined(), 0, "UNDEFINED");

    private int mValue;
    private long mBandwidth;
    private String mDescription;

    Bandwidth(int value, long bandwidth, String description)
    {
        mValue = value;
        mBandwidth = bandwidth;
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
     * Bandwidth (Hz)
     */
    public long getBandwidth()
    {
        return mBandwidth;
    }

    /**
     * Lookup the entry from a return code
     * @param value to lookup
     * @return entry or UNDEFINED if the code is not recognized
     */
    public static Bandwidth fromValue(int value)
    {
        for(Bandwidth status: Bandwidth.values())
        {
            if(status.getValue() == value)
            {
                return status;
            }
        }
        
        return UNDEFINED;
    }

    @Override
    public String toString()
    {
        return mDescription;
    }
}
