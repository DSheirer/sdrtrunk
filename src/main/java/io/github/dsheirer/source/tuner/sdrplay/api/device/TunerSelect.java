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
 * Tuner Select
 */
public enum TunerSelect
{
    TUNER_1(sdrplay_api_h.sdrplay_api_Tuner_A(), "TUNER 1"),
    TUNER_2(sdrplay_api_h.sdrplay_api_Tuner_B(), "TUNER 2"),
    TUNER_BOTH(sdrplay_api_h.sdrplay_api_Tuner_Both(), "TUNER 1 & 2"),
    NEITHER(sdrplay_api_h.sdrplay_api_Tuner_Neither(), "NEITHER");

    private int mValue;
    private String mDescription;

    TunerSelect(int value, String description)
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
     * @return entry or NEITHER if the code is not recognized
     */
    public static TunerSelect fromValue(int value)
    {
        for(TunerSelect status: TunerSelect.values())
        {
            if(status.getValue() == value)
            {
                return status;
            }
        }
        
        return NEITHER;
    }

    @Override
    public String toString()
    {
        return mDescription;
    }
}
