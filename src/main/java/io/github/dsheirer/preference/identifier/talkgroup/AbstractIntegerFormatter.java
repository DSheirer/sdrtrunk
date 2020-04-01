/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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

import io.github.dsheirer.preference.identifier.IntegerFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;

/**
 * Base formatter class for integer values
 */
public abstract class AbstractIntegerFormatter
{
    private final static Logger mLog = LoggerFactory.getLogger(AbstractIntegerFormatter.class);

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

    /**
     * Formats the integer value for display as a string.
     *
     * Subclass implementations should override this method to return formatted values.
     */
    public String format(int value)
    {
        return String.valueOf(value);
    }

    public abstract String format(int value, IntegerFormat integerFormat);

    /**
     * Parses an integer value from the formatted string value
     *
     * Subclass implementations should override this method to provide custom parsing of formatted values
     */
    public int parse(String formattedValue) throws ParseException
    {
        try
        {
            return Integer.parseInt(formattedValue.trim());
        }
        catch(Exception e)
        {
            //exception re-thrown below
        }

        mLog.error("Throwing a parse exception for value [" + formattedValue + "]");
        throw new ParseException("Error parsing integer value from [" + formattedValue + "]", 0);
    }
}
