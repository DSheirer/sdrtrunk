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
 * RSPdx High Dynamic Range (HDR) Mode Bandwidth
 */
public enum HdrModeBandwidth
{
    BANDWIDTH_0_200(sdrplay_api_h.sdrplay_api_RspDx_HDRMODE_BW_0_200(), 200_000, ".2 MHz"),
    BANDWIDTH_0_500(sdrplay_api_h.sdrplay_api_RspDx_HDRMODE_BW_0_500(), 500_000, ".5 MHz"),
    BANDWIDTH_1_200(sdrplay_api_h.sdrplay_api_RspDx_HDRMODE_BW_1_200(), 1_200_000, "1.2 MHz"),
    BANDWIDTH_1_700(sdrplay_api_h.sdrplay_api_RspDx_HDRMODE_BW_1_700(), 1_700_000, "1.7 MHz"),
    UNKNOWN(-1, 0, "UNKNOWN");

    private int mValue;
    private long mBandwidth;
    private String mDescription;

    HdrModeBandwidth(int value, long bandwidth, String description)
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
     * Bandwidth in Hertz
     */
    public long getBandwidth()
    {
        return mBandwidth;
    }

    /**
     * Lookup the entry from a return code
     * @param value to lookup
     * @return entry or UKNOWN if the code is not recognized
     */
    public static HdrModeBandwidth fromValue(int value)
    {
        for(HdrModeBandwidth status: HdrModeBandwidth.values())
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
