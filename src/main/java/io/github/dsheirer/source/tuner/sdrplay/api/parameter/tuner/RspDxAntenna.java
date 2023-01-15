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
 * RSP-Dx Antenna
 */
public enum RspDxAntenna
{
    ANTENNA_A(sdrplay_api_h.sdrplay_api_RspDx_ANTENNA_A(), "ANT A (1 kHz to 2 GHz)"),
    ANTENNA_B(sdrplay_api_h.sdrplay_api_RspDx_ANTENNA_B(), "ANT B (1 kHz to 2 GHz)"),
    ANTENNA_C(sdrplay_api_h.sdrplay_api_RspDx_ANTENNA_C(), "ANT C (1 kHz to 200 MHz)");

    private int mValue;
    private String mDescription;

    RspDxAntenna(int value, String description)
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
     * @return entry or ANTENNA A if the code is not recognized
     */
    public static RspDxAntenna fromValue(int value)
    {
        for(RspDxAntenna status: RspDxAntenna.values())
        {
            if(status.getValue() == value)
            {
                return status;
            }
        }
        
        return ANTENNA_A;
    }

    @Override
    public String toString()
    {
        return mDescription;
    }
}
