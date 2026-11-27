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

package io.github.dsheirer.module.decode.pager;

import java.util.Arrays;

/**
 * BCH(31,21) error detection and correction used by both POCSAG and FLEX paging codewords.  Corrects up to two bit
 * errors per codeword.
 *
 * The codeword is a 31-bit value where the first transmitted bit is in bit 30 (the highest polynomial degree).  The
 * 21 data bits occupy bits 30-10 and the 10 check bits occupy bits 9-0.  The generator polynomial is
 * x^10 + x^9 + x^8 + x^6 + x^5 + x^3 + 1.
 *
 * POCSAG transmits the codeword MSB first, so a 32-bit POCSAG codeword shifted right by one (dropping the even parity
 * bit) is already in this layout.  FLEX transmits the 21 data bits LSB first, followed by the check bits, so a FLEX
 * word with the first received bit in bit 0 must be bit-reversed (see {@link #reverse31(int)}) before correction.
 */
public final class BCH3121
{
    public static final int GENERATOR = 0x769;
    public static final int UNCORRECTABLE = -1;
    private static final int CODEWORD_MASK = 0x7FFFFFFF;
    private static final int[] ERROR_PATTERNS = new int[1024];

    static
    {
        Arrays.fill(ERROR_PATTERNS, UNCORRECTABLE);
        ERROR_PATTERNS[0] = 0;

        for(int x = 0; x < 31; x++)
        {
            ERROR_PATTERNS[syndrome(1 << x)] = 1 << x;
        }

        for(int x = 0; x < 31; x++)
        {
            for(int y = x + 1; y < 31; y++)
            {
                int pattern = (1 << x) | (1 << y);
                ERROR_PATTERNS[syndrome(pattern)] = pattern;
            }
        }
    }

    private BCH3121()
    {
    }

    /**
     * Calculates the 10-bit syndrome (remainder after division by the generator polynomial) of the codeword.
     * @param codeword with 31 bits in bits 30-0
     * @return syndrome, where 0 indicates a valid codeword.
     */
    public static int syndrome(int codeword)
    {
        int remainder = codeword & CODEWORD_MASK;

        for(int bit = 30; bit >= 10; bit--)
        {
            if(((remainder >>> bit) & 1) != 0)
            {
                remainder ^= GENERATOR << (bit - 10);
            }
        }

        return remainder & 0x3FF;
    }

    /**
     * Error pattern for the codeword.
     * @param codeword with 31 bits in bits 30-0
     * @return 0 when the codeword is valid, a bit mask with the one or two errored bit positions when the codeword is
     * correctable, or {@link #UNCORRECTABLE}
     */
    public static int getErrorPattern(int codeword)
    {
        return ERROR_PATTERNS[syndrome(codeword)];
    }

    /**
     * Encodes 21 data bits into a 31-bit codeword.
     * @param data with 21 bits in bits 20-0
     * @return codeword with the data in bits 30-10 and the check bits in bits 9-0
     */
    public static int encode(int data)
    {
        int shifted = (data & 0x1FFFFF) << 10;
        return shifted | syndrome(shifted);
    }

    /**
     * Reverses the order of the lower 31 bits so that bit 0 becomes bit 30.
     * @param value to reverse
     * @return reversed value
     */
    public static int reverse31(int value)
    {
        return Integer.reverse(value & CODEWORD_MASK) >>> 1;
    }
}
