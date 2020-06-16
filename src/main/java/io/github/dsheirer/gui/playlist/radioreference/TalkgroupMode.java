/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.radioreference;

/**
 * Enumeration of radio reference talkgroup modes
 *
 * Note: service uses trailing value of 'E' and 'e' for full or partial encryption.  Those are mapped to FE and PE
 * in the enumeration values.
 */
public enum TalkgroupMode
{
    A("Analog"),
    D("Digital"),
    M("Analog/Digital"),
    T("TDMA"),
    UNKNOWN("Unknown");

    private String mLabel;

    TalkgroupMode(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    public static TalkgroupMode lookup(String value)
    {
        if(value == null || value.isEmpty())
        {
            return TalkgroupMode.UNKNOWN;
        }

        switch(value)
        {
            case "A":
                return A;
            case "D":
                return D;
            case "M":
                return M;
            case "T":
                return T;
        }

        return TalkgroupMode.UNKNOWN;
    }
}
