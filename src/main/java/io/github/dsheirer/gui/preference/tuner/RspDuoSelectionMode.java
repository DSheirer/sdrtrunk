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

package io.github.dsheirer.gui.preference.tuner;

/**
 * RSPduo tuner select mode preferences.
 */
public enum RspDuoSelectionMode
{
    DUAL("Dual Tuner"),
    SINGLE_1("Single Tuner 1"),
    SINGLE_2("Single Tuner 2");

    private String mLabel;

    RspDuoSelectionMode(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Lookup the enum entry from the value.
     * @param value to match
     * @return matched value or (default) DUAL if the value couldn't be matched.
     */
    public static RspDuoSelectionMode fromValue(String value)
    {
        try
        {
            return RspDuoSelectionMode.valueOf(value);
        }
        catch(Exception e)
        {
            //Do nothing
        }

        return DUAL;
    }
}
