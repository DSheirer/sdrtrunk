/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.util;

/**
 * Utilities for working with bytes and byte arrays
 */
public class ByteUtil
{
    /**
     * Converts a byte array to an integer using big endian format.
     * @param bytes containing four bytes.
     * @param offset into the byte array to start parsing.
     * @return signed integer value.
     */
    public static int toInteger(byte[] bytes, int offset)
    {
        if(bytes == null || bytes.length < (offset + 4))
        {
            throw new IllegalArgumentException("Conversion to integer requires byte array with at least 4 bytes - " +
                    "length:" + bytes.length + " offset:" + offset);
        }

        int value = (bytes[offset + 3] & 0xFF) << 24;
        value += (bytes[offset + 2] & 0xFF) << 16;
        value += (bytes[offset + 1] & 0xFF) << 8;
        value += (bytes[offset] & 0xFF);

        return value;
    }

    /**
     * Formats the byte array as a string of hexadecimal values.
     * @param bytes to format
     * @return hex string
     */
    public static String toHexString(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder();

        for(byte b : bytes)
        {
            sb.append(String.format("%02X", b));
        }

        return sb.toString();
    }

    public static String toHexString(int[] intBytes)
    {
        byte[] converted = new byte[intBytes.length];

        for(int x = 0; x < intBytes.length; x++)
        {
            converted[x] = (byte)(intBytes[x] & 0xFF);
        }

        return toHexString(converted);
    }
}
