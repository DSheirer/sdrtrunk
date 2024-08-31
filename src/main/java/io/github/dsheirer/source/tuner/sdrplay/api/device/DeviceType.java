/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

import io.github.dsheirer.source.tuner.sdrplay.api.v3_15.sdrplay_api_h;

/**
 * RSP Device type
 */
public enum DeviceType
{
    RSP1(sdrplay_api_h.SDRPLAY_RSP1_ID(), "RSP1"),
    RSP1A(sdrplay_api_h.SDRPLAY_RSP1A_ID(), "RSP1A"),
    RSP1B(sdrplay_api_h.SDRPLAY_RSP1B_ID(), "RSP1B"),
    RSP2(sdrplay_api_h.SDRPLAY_RSP2_ID(), "RSP2"),
    RSPduo(sdrplay_api_h.SDRPLAY_RSPduo_ID(), "RSPduo"),
    RSPdx(sdrplay_api_h.SDRPLAY_RSPdx_ID(), "RSPdx"),
    RSPdxR2(sdrplay_api_h.SDRPLAY_RSPdxR2_ID(), "RSPdxR2"),
    UNKNOWN(Integer.MIN_VALUE, "UNKNOWN");

    private final int mValue;
    private final String mDescription;

    DeviceType(int value, String description)
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
    public static DeviceType fromValue(int value)
    {
        for(DeviceType status: DeviceType.values())
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
