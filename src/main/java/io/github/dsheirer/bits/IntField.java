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
public record IntField(int start, int end)
{
    public IntField
    {
        if(end - start > 32)
        {
            throw new IllegalArgumentException("Integer field length [" + (end - start) + "] cannot exceed 32-bits for an integer");
        }

        if(end < start)
        {
            throw new IllegalArgumentException("Integer field start index must be less than end index");
        }
    }

    /**
     * Width of the field in bit positions
     * @return width
     */
    public int width()
    {
        return end() - start() + 1;
    }

    /**
     * Utility constructor method.
     * @param start index (inclusive)
     * @param end index (inclusive)
     * @return constructed bit field.
     */
    public static IntField range(int start, int end)
    {
        return new IntField(start, end);
    }

    /**
     * Utility constructor method for a field with one four bits of length.
     * @param start index (inclusive)
     * @return constructed bit field.
     */
    public static IntField length4(int start)
    {
        return new IntField(start, (start + 3));
    }

    /**
     * Utility constructor method for a field with one octet of length.
     * @param start index (inclusive)
     * @return constructed bit field.
     */
    public static IntField length8(int start)
    {
        return new IntField(start, (start + 7));
    }

    /**
     * Utility constructor method for a field with 12 bits of length.
     * @param start index (inclusive)
     * @return constructed bit field.
     */
    public static IntField length12(int start)
    {
        return new IntField(start, (start + 11));
    }

    /**
     * Utility constructor method for a field with two octets of length.
     * @param start index (inclusive)
     * @return constructed bit field.
     */
    public static IntField length16(int start)
    {
        return new IntField(start, (start + 15));
    }

    /**
     * Utility constructor method for a field with 20 bits of length.
     * @param start index (inclusive)
     * @return constructed bit field.
     */
    public static IntField length20(int start)
    {
        return new IntField(start, (start + 19));
    }

    /**
     * Utility constructor method for a field with two octets of length.
     * @param start index (inclusive)
     * @return constructed bit field.
     */
    public static IntField length24(int start)
    {
        return new IntField(start, (start + 23));
    }

    /**
     * Utility constructor method for a field with three octets of length.
     * @param start index (inclusive)
     * @return constructed bit field.
     */
    public static IntField length32(int start)
    {
        return new IntField(start, (start + 31));
    }
}
