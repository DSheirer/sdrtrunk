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
 * Enumeration of talker alias data formatting options.
 *
 * ETSI 102 361-2 v2.4.1 paragraph 7.2.18
 */
public enum TalkerAliasDataFormat
{
    BIT_7(0, 7, "ASCII-7"),
    BIT_8(1, 8, "ASCII-8"),
    UTF_8(2, 8, "UTF-8"),
    UNICODE_UTF_16_BE(3, 16, "UNICODE-16");

    private int mValue;
    private int mBitsPerCharacter;
    private String mLabel;

    /**
     * Constructs an instance
     * @param value ordinal
     */
    TalkerAliasDataFormat(int value, int bitsPerCharacter, String label)
    {
        mValue = value;
        mBitsPerCharacter = bitsPerCharacter;
        mLabel = label;
    }

    /**
     * Ordinal value of the format
     * @return ordinal value.
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Number of bits per character for the format.
     * @return bits per character
     */
    public int getBitsPerCharacter()
    {
        return mBitsPerCharacter;
    }

    /**
     * Lookup the format from the ordinal value.
     * @param value to lookup in range 0-3
     * @return format for the value.
     */
    public static TalkerAliasDataFormat fromValue(int value)
    {
        if(value < 0 || value > 3)
        {
            throw new IllegalArgumentException("Invalid value - out of range (0-3): " + value);
        }

        return TalkerAliasDataFormat.values()[value];
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
