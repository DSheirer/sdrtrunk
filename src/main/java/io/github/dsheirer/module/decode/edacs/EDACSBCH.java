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

package io.github.dsheirer.module.decode.edacs;

/**
 * The BCH (40,28) code protecting each EDACS outbound control channel message - 28 data bits followed
 * by 12 check bits, generator polynomial 0x1539.  Found against MBTA (853.725 MHz, 2026-11-09): 99.2% of 63,111
 * majority-voted messages are codewords of this generator.
 */
public final class EDACSBCH
{
    public static final int GENERATOR = 0x1539;
    public static final int DATA_BITS = 28;
    public static final int CHECK_BITS = 12;

    private EDACSBCH()
    {
    }

    /**
     * Remainder of a 40-bit word divided by the generator - zero for a valid codeword.
     */
    public static int syndrome(long codeword)
    {
        long v = codeword & 0xFFFFFFFFFFL;

        for(int bit = 39; bit >= CHECK_BITS; bit--)
        {
            if(((v >> bit) & 1) == 1)
            {
                v ^= ((long)GENERATOR) << (bit - CHECK_BITS);
            }
        }

        return (int)v;
    }

    /**
     * Indicates the 40-bit word is a valid codeword.
     */
    public static boolean isValid(long codeword)
    {
        return syndrome(codeword) == 0;
    }

    /**
     * Encodes 28 data bits as a 40-bit codeword.
     */
    public static long encode(int data)
    {
        long shifted = ((long)(data & 0xFFFFFFF)) << CHECK_BITS;
        return shifted | syndrome(shifted);
    }
}
