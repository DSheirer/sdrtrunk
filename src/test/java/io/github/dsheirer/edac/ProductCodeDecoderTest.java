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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the product code minimum distance decoder.  The DMR BPTC tests cover error correction capability.
 */
public class ProductCodeDecoderTest
{
    //Hamming(7,4,3) rows
    private static final int[] HAMMING_7 = new int[]{0x3, 0x5, 0x6, 0x7, 0x4, 0x2, 0x1};

    /**
     * Test: column codes whose final rows are not distinct single bit (systematic) parity checks are rejected.
     */
    @Test
    void testColumnCodeValidation()
    {
        assertThrows(IllegalArgumentException.class, () -> new ProductCodeDecoder(HAMMING_7, new int[]{1, 3}, 2));
        assertThrows(IllegalArgumentException.class, () -> new ProductCodeDecoder(HAMMING_7, new int[]{3, 1, 1}, 2));
        assertThrows(IllegalArgumentException.class, () -> new ProductCodeDecoder(HAMMING_7, new int[]{1}, 2));
    }

    /**
     * Test: Hamming(7,4,3) rows with even parity columns (minimum distance 6) corrects 2 bit errors and leaves the
     * rows unchanged when uncorrectable.
     */
    @Test
    void testDecode()
    {
        ProductCodeDecoder decoder = new ProductCodeDecoder(HAMMING_7, new int[]{1, 1, 1}, 3);
        //Row codewords 0x61 (0x3 ^ 0x2 ^ 0x1 = 0) and 0x52 (0x5 ^ 0x4 ^ 0x1 = 0) with the column parity row 0x33
        int[] codeword = new int[]{0x61, 0x52, 0x33};
        assertTrue(decoder.isValid(codeword));
        assertEquals(0, decoder.decode(codeword.clone()));

        //Two errors in the same column: no column parity errors
        int[] received = new int[]{0x61 ^ 0x01, 0x52 ^ 0x01, 0x33};
        assertFalse(decoder.isValid(received));
        assertEquals(2, decoder.decode(received));
        assertArrayEquals(codeword, received);

        //Two errors in the same row, beyond the row code correction capability
        received = new int[]{0x61, 0x52, 0x33 ^ 0x30};
        assertEquals(2, decoder.decode(received));
        assertArrayEquals(codeword, received);

        //Uncorrectable within the maximum error count: unchanged
        ProductCodeDecoder limited = new ProductCodeDecoder(HAMMING_7, new int[]{1, 1, 1}, 1);
        received = new int[]{0x61 ^ 0x01, 0x52 ^ 0x01, 0x33};
        int[] copy = received.clone();
        assertEquals(ProductCodeDecoder.UNCORRECTABLE, limited.decode(received));
        assertArrayEquals(copy, received);
    }
}
