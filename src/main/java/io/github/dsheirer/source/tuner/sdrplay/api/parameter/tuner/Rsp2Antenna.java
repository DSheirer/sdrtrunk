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
 * RSP-2 Antenna
 */
public enum Rsp2Antenna
{
    ANTENNA_A(sdrplay_api_h.sdrplay_api_Rsp2_ANTENNA_A(), "ANT A"),
    ANTENNA_B(sdrplay_api_h.sdrplay_api_Rsp2_ANTENNA_B(), "ANT B");

    private int mValue;
    private String mDescription;

    Rsp2Antenna(int value, String description)
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
    public static Rsp2Antenna fromValue(int value)
    {
        for(Rsp2Antenna status: Rsp2Antenna.values())
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
