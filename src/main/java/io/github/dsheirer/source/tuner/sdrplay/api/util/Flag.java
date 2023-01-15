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

package io.github.dsheirer.source.tuner.sdrplay.api.util;

/**
 * Utility for integer and byte values representing true/false flags.
 */
public class Flag
{
    public static final byte TRUE = 0x01;
    public static final byte FALSE = 0x00;

    /**
     * Evaluates the boolean value false (0x00) or true
     * @param value to evaluate
     * @return true of the value is non-zero
     */
    public static boolean evaluate(byte value)
    {
        return value != FALSE;
    }

    /**
     * Evaluates the boolean value false (0) or true
     * @param value to evaluate
     * @return true of the value is non-zero
     */
    public static boolean evaluate(int value)
    {
        return value != 0;
    }

    /**
     * Returns a byte value representing the boolean value.
     * @param value to lookup
     * @return byte flag value
     */
    public static byte of(boolean value)
    {
        return value ? TRUE : FALSE;
    }
}
