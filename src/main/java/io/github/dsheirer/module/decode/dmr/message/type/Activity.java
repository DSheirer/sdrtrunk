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

package io.github.dsheirer.module.decode.dmr.message.type;

/**
 * Activity enumeration
 *
 * Defined in: TS 102 361-2 7.1.3.2 Activity Update
 */
public enum Activity
{
    IDLE(0, "IDLE"),
    RESERVED_1(1, "RESERVED 1"),
    GROUP_CSBK(2, "GROUP CSBK"),
    INDIVIDUAL_CSBK(3, "INDIV CSBK"),
    RESERVED_4(4, "RESERVED 4"),
    RESERVED_5(5, "RESERVED 5"),
    RESERVED_6(6, "RESERVED 6"),
    RESERVED_7(7, "RESERVED 7"),
    GROUP_VOICE(8, "GROUP VOICE"),
    INDIVIDUAL_VOICE(9, "INDIV VOICE"),
    INDIVIDUAL_DATA(10, "INDIV DATA"),
    GROUP_DATA(11, "GROUP DATA"),
    EMERGENCY_GROUP_VOICE(12, "EMERG GROUP VOICE"),
    EMERGENCY_INDIVIDUAL_VOICE(13, "EMERG INDIV VOICE"),
    RESERVED_14(14, "RESERVED 14"),
    RESERVED_15(15, "RESERVED 15"),
    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    /**
     * Constructs an instance
     * @param value of the activity item
     * @param label pretty string
     */
    Activity(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Pretty version of the entry
     */
    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Lookup the activity from a value
     * @param value 0-15
     * @return entry or UNKNOWN
     */
    public static Activity fromValue(int value)
    {
        if(0 <= value && value <= 15)
        {
            return Activity.values()[value];
        }

        return UNKNOWN;
    }
}
