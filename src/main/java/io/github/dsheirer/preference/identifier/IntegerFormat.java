/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.preference.identifier;

import java.util.EnumSet;

/**
 * Options for formatting of integer values.
 */
public enum IntegerFormat
{
    DECIMAL("Decimal"),
    FORMATTED("Formatted"),
    HEXADECIMAL("Hexadecimal");

    private String mLabel;

    IntegerFormat(String label)
    {
        mLabel = label;
    }

    public String toString()
    {
        return mLabel;
    }

    public static EnumSet<IntegerFormat> DECIMAL_HEXADECIMAL = EnumSet.of(DECIMAL, HEXADECIMAL);
    public static EnumSet<IntegerFormat> DECIMAL_FORMATTED = EnumSet.of(DECIMAL, FORMATTED);
}
