/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
 * DMR Position Error enumeration.
 *
 * ETSI 102 361-2 7.2.15 Position Error
 */
public enum PositionError
{
    ERROR_0("LESS THAN 2 METERS"),
    ERROR_1("LESS THAN 20 METERS"),
    ERROR_2("LESS THAN 200 METERS"),
    ERROR_3("LESS THAN 2 KILOMETERS"),
    ERROR_4("LESS THAN 20 KILOMETERS"),
    ERROR_5("LESS THAN 200 KILOMETERS"),
    ERROR_6("MORE THAN 200 KILOMETERS"),
    UNKNOWN("NOT KNOWN");

    private String mLabel;

    /**
     * Constructs an instance
     * @param label descriptive
     */
    PositionError(String label)
    {
        mLabel = label;
    }

    /**
     * Descriptive label
     */
    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Lookup the position error from the value
     * @param value 0 - 7
     * @return enum entry or UNKNOWN
     */
    public static PositionError fromValue(int value)
    {
        if(0 <= value && value <= 7)
        {
            return PositionError.values()[value];
        }

        return UNKNOWN;
    }
}
