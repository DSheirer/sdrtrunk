/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
 * Hytera data encoding format values.
 */
public enum HyteraEncodeFormat
{
    BINARY(0),
    MOBILE_SUBSCRIBER_NUMBER(1),
    BINARY_CODED_DECIMAL(2),
    ISO_7(3),
    ISO_8(4),
    NMEA_GPS(5),
    INTERNET_PROTOCOL(6),
    UNICODE(7),
    BYTES_8_BITS(8),
    GBK(10), //GB2312 Simplified Chinese Characters
    UNKNOWN(-1);

    private int mValue;

    /**
     * Constructs an instance.
     * @param value
     */
    HyteraEncodeFormat(int value)
    {
        mValue = value;
    }

    /**
     * Coded value for this format
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Lookup the element from the value.
     * @param value to lookup
     * @return element or UNKNOWN
     */
    public static HyteraEncodeFormat fromValue(int value)
    {
        if(0 <= value && value <= 8)
        {
            return HyteraEncodeFormat.values()[value];
        }
        else if(value == 10)
        {
            return GBK;
        }

        return UNKNOWN;
    }
}
