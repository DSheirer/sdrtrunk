/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

/**
 * SR structure field enumeration.  Part 1-A, paragraph 6.3.1
 */
public enum Structure
{
    CONTROL_NOT_SUPERFRAME_HEAD_SINGLE("SF- SINGLE"),
    CONTROL_NOT_SUPERFRAME_HEAD_DOUBLE("SF- DOUBLE"),
    CONTROL_SUPERFRAME_HEAD_SINGLE("SFH SINGLE"),
    CONTROL_SUPERFRAME_HEAD_DOUBLE("SFH DOUBLE"),

    SACCH_4_OF_4_LAST_OR_SINGLE("SACCH 4/4"),
    SACCH_3_OF_4("SACCH 3/4"),
    SACCH_2_OF_4("SACCH 2/4"),
    SACCH_1_OF_4("SACCH 1/4"),

    UNKNOWN("UNKNOWN");

    private String mLabel;

    /**
     * Constructs an instance
     * @param label to display for the value.
     */
    Structure(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Lookup the structure entry from the value for a control channel.
     * @param value to lookup
     * @return matching entry or UNKNOWN
     */
    public static Structure fromControlValue(int value)
    {
        return switch(value)
        {
            case 0 -> CONTROL_NOT_SUPERFRAME_HEAD_SINGLE;
            case 1 -> CONTROL_NOT_SUPERFRAME_HEAD_DOUBLE;
            case 2 -> CONTROL_SUPERFRAME_HEAD_SINGLE;
            case 3 -> CONTROL_SUPERFRAME_HEAD_DOUBLE;
            default -> UNKNOWN;
        };
    }

    /**
     * Lookup the structure entry from the value for a control channel.
     * @param value to lookup
     * @return matching entry or UNKNOWN
     */
    public static Structure fromTrafficValue(int value)
    {
        return switch(value)
        {
            case 0 -> SACCH_4_OF_4_LAST_OR_SINGLE;
            case 1 -> SACCH_3_OF_4;
            case 2 -> SACCH_2_OF_4;
            case 3 -> SACCH_1_OF_4;
            default -> UNKNOWN;
        };
    }
}
