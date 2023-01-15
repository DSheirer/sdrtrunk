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
 * Intermediate Frequency (IF) Mode / Frequency
 */
public enum IfMode
{
    IF_ZERO(sdrplay_api_h.sdrplay_api_IF_Zero(), "ZIF 0.000 MHz"),
    IF_450(sdrplay_api_h.sdrplay_api_IF_0_450(), "0.450 MHz"),
    IF_1620(sdrplay_api_h.sdrplay_api_IF_1_620(), "1.620 MHz"),
    IF_2048(sdrplay_api_h.sdrplay_api_IF_2_048(), "2.048 MHz"),
    UNDEFINED(sdrplay_api_h.sdrplay_api_IF_Undefined(), "UNDEFINED");

    private int mValue;
    private String mDescription;

    IfMode(int value, String description)
    {
        mValue = value;
        mDescription = description;
    }

    /**
     * Indicates the wideband enable decimation setting to use for this IF value.
     */
    public boolean isWidebandSignal()
    {
        return this.equals(IF_ZERO);
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
    public static IfMode fromValue(int value)
    {
        for(IfMode status: IfMode.values())
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
