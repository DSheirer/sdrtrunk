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

package io.github.dsheirer.settings.identifier.talkgroup;

import io.github.dsheirer.settings.identifier.IntegerFormat;

/**
 * Formatter for POCSAG and FLEX pager capcodes.
 */
public class PagerCapcodeFormatter extends AbstractIntegerFormatter
{
    public static final int DECIMAL_WIDTH = 7;
    public static final int HEXADECIMAL_WIDTH = 6;

    /**
     * Formats a pager capcode with the configured display width.
     */
    public static String format(int value, IntegerFormat format, boolean fixedWidth)
    {
        return switch(format)
        {
            case DECIMAL, FORMATTED -> fixedWidth ? toDecimal(value, DECIMAL_WIDTH) : Integer.toString(value);
            case HEXADECIMAL -> fixedWidth ? toHex(value, HEXADECIMAL_WIDTH) : toHex(value);
        };
    }

    @Override
    public String format(int value, IntegerFormat format)
    {
        return format(value, format, false);
    }
}
