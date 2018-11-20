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

package io.github.dsheirer.preference.identifier.talkgroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base formatter class for integer values
 */
public class IntegerFormatter
{
    private final static Logger mLog = LoggerFactory.getLogger(IntegerFormatter.class);

    public static String toHex(int value)
    {
        return Integer.toHexString(value).toUpperCase();
    }

    public static String toHex(int value, int width)
    {
        return String.format("%0" + width + "X", value);
    }

    public static String toDecimal(int value, int width)
    {
        return String.format("%0" + width + "d", value);
    }
}
