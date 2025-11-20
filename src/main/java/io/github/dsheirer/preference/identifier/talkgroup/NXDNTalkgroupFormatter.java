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

package io.github.dsheirer.preference.identifier.talkgroup;

import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNTalkgroupIdentifier;
import io.github.dsheirer.preference.identifier.IntegerFormat;

/**
 * Talkgroup formatter for NXDN protocol
 */
public class NXDNTalkgroupFormatter extends AbstractIntegerFormatter
{
    public static final int TYPE_D_REPEATER_WIDTH = 2;
    public static final int TYPE_D_VALUE_WIDTH = 4;
    public static final String TYPE_D_SEPARATOR = "-";
    public static final int GROUP_DECIMAL_WIDTH = 5;
    public static final int UNIT_DECIMAL_WIDTH = 5;
    public static final int GROUP_HEXADECIMAL_WIDTH = 4;
    public static final int UNIT_HEXADECIMAL_WIDTH = 4;

    /**
     * Formats the individual or group identifier to the specified format and width.
     */
    public static String format(TalkgroupIdentifier identifier, IntegerFormat format, boolean fixedWidth)
    {
        if(identifier instanceof NXDNTalkgroupIdentifier nti && nti.isTypeD())
        {
            return toTypeD(nti.getTypeDHomeRepeater(), nti.getTypeDTalkgroup(), fixedWidth);
        }

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
     * Formats the radio identifier to the specified format and width.
     */
    public static String format(RadioIdentifier identifier, IntegerFormat format, boolean fixedWidth)
    {
        if(identifier instanceof NXDNRadioIdentifier nri && nri.isTypeD())
        {
            return toTypeD(nri.getTypeDHomeRepeater(), nri.getTypeDRadio(), fixedWidth);
        }

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
    public String format(int value, IntegerFormat integerFormat)
    {
        switch(integerFormat)
        {
            case DECIMAL:
            case FORMATTED:
                return format(value);
            case HEXADECIMAL:
                return toHex(value);
            default:
                throw new IllegalArgumentException("Unrecognized integer format: " + integerFormat);
        }
    }

    /**
     * Formats a Type-D identifier
     * @param homeRepeater for the ID
     * @param value of the radio or talkgroup
     * @param fixedWidth flag
     * @return formatted identifier
     */
    public static String toTypeD(int homeRepeater, int value, boolean fixedWidth)
    {
        if(fixedWidth)
        {
            return toDecimal(homeRepeater, TYPE_D_REPEATER_WIDTH) + TYPE_D_SEPARATOR +
                    toDecimal(value, TYPE_D_VALUE_WIDTH);
        }
        else
        {
            return homeRepeater + TYPE_D_SEPARATOR + value;
        }
    }
}
