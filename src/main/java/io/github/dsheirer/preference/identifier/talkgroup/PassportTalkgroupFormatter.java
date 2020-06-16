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

import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.preference.identifier.IntegerFormat;

public class PassportTalkgroupFormatter extends AbstractIntegerFormatter
{
    public static final int GROUP_DECIMAL_WIDTH = 5;
    public static final int GROUP_HEXADECIMAL_WIDTH = 4;

    public static final int UNIT_DECIMAL_WIDTH = 7;
    public static final int UNIT_HEXADECIMAL_WIDTH = 6;

    /**
     * Formats the individual or group identifier to the specified format and width.
     */
    public static String format(TalkgroupIdentifier identifier, IntegerFormat format, boolean fixedWidth)
    {
        if(fixedWidth)
        {
            switch(format)
            {
                case DECIMAL:
                case FORMATTED:
                    return toDecimal(identifier.getValue(), GROUP_DECIMAL_WIDTH);
                case HEXADECIMAL:
                    return toHex(identifier.getValue(), GROUP_HEXADECIMAL_WIDTH);
                default:
                    throw new IllegalArgumentException("Unrecognized integer format: " + format);
            }
        }
        else
        {
            switch(format)
            {
                case DECIMAL:
                case FORMATTED:
                    return identifier.getValue().toString();
                case HEXADECIMAL:
                    return toHex(identifier.getValue());
                default:
                    throw new IllegalArgumentException("Unrecognized integer format: " + format);
            }
        }
    }

    /**
     * Formats the individual or group identifier to the specified format and width.
     */
    public static String format(RadioIdentifier identifier, IntegerFormat format, boolean fixedWidth)
    {
        if(fixedWidth)
        {
            switch(format)
            {
                case DECIMAL:
                case FORMATTED:
                    return toDecimal(identifier.getValue(), UNIT_DECIMAL_WIDTH);
                case HEXADECIMAL:
                    return toHex(identifier.getValue(), UNIT_HEXADECIMAL_WIDTH);
                default:
                    throw new IllegalArgumentException("Unrecognized integer format: " + format);
            }
        }
        else
        {
            switch(format)
            {
                case DECIMAL:
                case FORMATTED:
                    return identifier.getValue().toString();
                case HEXADECIMAL:
                    return toHex(identifier.getValue());
                default:
                    throw new IllegalArgumentException("Unrecognized integer format: " + format);
            }
        }
    }

    @Override
    public String format(int value, IntegerFormat format)
    {
        switch(format)
        {
            case DECIMAL:
            case FORMATTED:
                return format(value);
            case HEXADECIMAL:
                return toHex(value);
            default:
                throw new IllegalArgumentException("Unrecognized integer format: " + format);
        }
    }
}
