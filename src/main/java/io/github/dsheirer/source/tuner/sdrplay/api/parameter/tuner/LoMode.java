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
 * Local Oscillator Mode
 */
public enum LoMode
{
    AUTO(sdrplay_api_h.sdrplay_api_LO_Auto(), "AUTO"),
    MHZ_120(sdrplay_api_h.sdrplay_api_LO_120MHz(), "120 MHz"),
    MHZ_144(sdrplay_api_h.sdrplay_api_LO_144MHz(), "144 MHz"),
    MHZ_168(sdrplay_api_h.sdrplay_api_LO_168MHz(), "168 MHz"),
    UNDEFINED(sdrplay_api_h.sdrplay_api_LO_Undefined(), "UNDEFINED");

    private int mValue;
    private String mDescription;

    LoMode(int value, String description)
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
    public static LoMode fromValue(int value)
    {
        for(LoMode status: LoMode.values())
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
