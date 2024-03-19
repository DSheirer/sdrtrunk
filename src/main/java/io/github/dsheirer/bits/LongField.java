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

package io.github.dsheirer.bits;

/**
 * Defines a contiguous bit field within a binary message.
 * @param start bit index, inclusive.
 * @param end bit index, inclusive.
 */
public record LongField(int start, int end)
{
    public LongField
    {
        if(end - start > 64)
        {
            throw new IllegalArgumentException("Long field length [" + (end - start) + "] cannot exceed 64-bits for a long");
        }

        if(end < start)
        {
            throw new IllegalArgumentException("Long field start index must be less than end index");
        }
    }

    /**
     * Utility constructor method.
     * @param start index (inclusive)
     * @param end index (inclusive)
     * @return constructed bit field.
     */
    public static LongField range(int start, int end)
    {
        return new LongField(start, end);
    }

    /**
     * Utility constructor method for a field with one four bits of length.
     * @param start index (inclusive)
     * @return constructed bit field.
     */
    public static LongField length4(int start)
    {
        return new LongField(start, (start + 3));
    }

    /**
     * Utility constructor method for a field with one octet of length.
     * @param start index (inclusive)
     * @return constructed bit field.
     */
    public static LongField length8(int start)
    {
        return new LongField(start, (start + 7));
    }

    /**
     * Utility constructor method for a field with 12 bits of length.
     * @param start index (inclusive)
     * @return constructed bit field.
     */
    public static LongField length12(int start)
    {
        return new LongField(start, (start + 11));
    }

    /**
     * Utility constructor method for a field with two octets of length.
     * @param start index (inclusive)
     * @return constructed bit field.
     */
    public static LongField length16(int start)
    {
        return new LongField(start, (start + 15));
    }

    /**
     * Utility constructor method for a field with 20 bits of length.
     * @param start index (inclusive)
     * @return constructed bit field.
     */
    public static LongField length20(int start)
    {
        return new LongField(start, (start + 19));
    }

    /**
     * Utility constructor method for a field with two octets of length.
     * @param start index (inclusive)
     * @return constructed bit field.
     */
    public static LongField length24(int start)
    {
        return new LongField(start, (start + 23));
    }

    /**
     * Utility constructor method for a field with three octets of length.
     * @param start index (inclusive)
     * @return constructed bit field.
     */
    public static LongField length32(int start)
    {
        return new LongField(start, (start + 31));
    }
}
