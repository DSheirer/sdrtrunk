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

package io.github.dsheirer.edac;

import io.github.dsheirer.bits.BinaryMessage;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit tests for the DMR Hamming codes: every single bit error in every codeword is located.
 */
public class HammingTest
{
    private static final int[] INDICES_13 = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};

    private static BinaryMessage toMessage(int word, int length)
    {
        BinaryMessage message = new BinaryMessage(length);

        for(int i = 0; i < length; i++)
        {
            if((word & (1 << i)) != 0)
            {
                message.set(i);
            }
        }

        return message;
    }

    /**
     * Asserts the code has the expected number of codewords and that the locator finds each single bit error.
     */
    private static void assertSingleErrorsLocated(int length, int informationLength, ToIntFunction<BinaryMessage> syndrome,
                                                  ToIntFunction<BinaryMessage> locator)
    {
        int[] checks = ProductCodeDecoder.getParityChecks(length, syndrome);
        int[] codewords = IntStream.range(0, 1 << length)
                .filter(word -> ProductCodeDecoder.getSyndrome(word, checks) == 0).toArray();
        assertEquals(1 << informationLength, codewords.length);

        for(int codeword: codewords)
        {
            assertEquals(IHamming.NO_ERRORS, locator.applyAsInt(toMessage(codeword, length)));

            for(int i = 0; i < length; i++)
            {
                assertEquals(i, locator.applyAsInt(toMessage(codeword ^ (1 << i), length)),
                        "Codeword " + Integer.toHexString(codeword) + " bit " + i);
            }
        }
    }

    @Test
    void testHamming13()
    {
        assertSingleErrorsLocated(13, 9, m -> Hamming13.getSyndrome(m, INDICES_13),
                m -> Hamming13.getErrorIndex(m, INDICES_13));
        assertSingleErrorsLocated(13, 9, m -> Hamming13.getSyndrome(m, INDICES_13), m -> Hamming13.getErrorIndex(m, 0));
    }

    /**
     * Test: the two syndromes that don't match a single bit error are flagged as multiple errors.
     */
    @Test
    void testHamming13MultipleErrors()
    {
        int[] checks = ProductCodeDecoder.getParityChecks(13, m -> Hamming13.getSyndrome(m, INDICES_13));
        int flagged = 0;

        for(int a = 0; a < 13; a++)
        {
            for(int b = a + 1; b < 13; b++)
            {
                int syndrome = checks[a] ^ checks[b];

                if(syndrome == 9 || syndrome == 13)
                {
                    BinaryMessage message = toMessage((1 << a) | (1 << b), 13);
                    assertEquals(IHamming.MULTIPLE_ERRORS, Hamming13.getErrorIndex(message, 0));
                    assertEquals(IHamming.MULTIPLE_ERRORS, Hamming13.getErrorIndex(message, INDICES_13));
                    flagged++;
                }
            }
        }

        assertEquals(12, flagged);
    }

    @Test
    void testHamming15()
    {
        assertSingleErrorsLocated(15, 11, m -> Hamming15.getSyndrome(m, 0), m -> Hamming15.getErrorIndex(m, 0));
    }

    /**
     * Test: single bit errors are located in every position, including the final parity bit, and every double bit
     * error is detected.
     */
    @Test
    void testHamming16()
    {
        Hamming16 hamming = new Hamming16();
        assertSingleErrorsLocated(16, 11, m -> Hamming16.getSyndrome(m, 0), m -> hamming.getErrorIndex(m, 0));

        for(int a = 0; a < 16; a++)
        {
            for(int b = a + 1; b < 16; b++)
            {
                assertEquals(IHamming.MULTIPLE_ERRORS, hamming.getErrorIndex(toMessage((1 << a) | (1 << b), 16), 0));
            }
        }
    }

    /**
     * Test: single bit errors are located in every position, including the final parity bit for both even and odd
     * weight codewords.
     */
    @Test
    void testHamming17()
    {
        Hamming17 hamming = new Hamming17();
        assertSingleErrorsLocated(17, 12, m -> Hamming17.getSyndrome(m, 0), m -> hamming.getErrorIndex(m, 0));
    }
}
