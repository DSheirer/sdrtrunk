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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.control;

import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_h;
import java.util.EnumSet;

/**
 * Automatic Gain Control (AGC) Control (mode)
 */
public enum AgcMode
{
    //Deprecated modes ... only use the ENABLE or DISABLE, in conjunction with the
    AGC_100_HZ(sdrplay_api_h.sdrplay_api_AGC_100HZ(), "Deprecated - 100 Hz"),
    AGC_50_HZ(sdrplay_api_h.sdrplay_api_AGC_50HZ(), "Deprecated 50 Hz"),
    AGC_5_HZ(sdrplay_api_h.sdrplay_api_AGC_5HZ(), "Deprecated 5 Hz"),

    DISABLE(sdrplay_api_h.sdrplay_api_AGC_DISABLE(), "DISABLE"),
    ENABLE(sdrplay_api_h.sdrplay_api_AGC_CTRL_EN(), "ENABLE");

    private int mValue;
    private String mDescription;

    AgcMode(int value, String description)
    {
        mValue = value;
        mDescription = description;
    }

    public static EnumSet<AgcMode> SUPPORTED_MODES = EnumSet.of(ENABLE, DISABLE);

    /**
     * Numeric value
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Indicates if the mode is a supported mode.
     * @return true if it's a supported mode.
     */
    public boolean isSupported()
    {
        return SUPPORTED_MODES.contains(this);
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
                //legacy support in case some users had the deprecated values stored in their tuner configs.  In
                //this situation we return ENABLED instead of one of these deprecated values.
                if(status.isSupported())
                {
                    return status;
                }
                else
                {
                    return ENABLE;
                }
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
