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

import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReedSolomon_63_ErasureDecoderTest
{
    private static final int[] FACCH_PUNCTURED = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] SACCH_PUNCTURED = {0, 1, 2, 3, 4, 5};
    private static final int[] NONE = {};

    /** A codeword with the top `shortened` information symbols zero, the rest pseudo-random. */
    private static int[] codeword(long seed, int shortened)
    {
        Random random = new Random(seed);
        int[] info = new int[35];

        for(int i = shortened; i < 35; i++)
        {
            info[i] = random.nextInt(64);
        }

        return ReedSolomon63TestEncoder.encode(info);
    }

    /** Copies the codeword, zeroes the punctured positions and flips `errors` distinct other symbols. */
    private static int[] damage(int[] codeword, int[] punctured, int errors, long seed)
    {
        int[] received = codeword.clone();

        for(int p : punctured)
        {
            received[p] = 0;
        }

        Random random = new Random(seed);
        boolean[] hit = new boolean[63];

        for(int p : punctured)
        {
            hit[p] = true;
        }

        for(int e = 0; e < errors; e++)
        {
            int position;

            do
            {
                position = random.nextInt(63);
            }
            while(hit[position]);

            hit[position] = true;
            received[position] ^= 1 + random.nextInt(63);
        }

        return received;
    }

    private static void assertCorrects(int[] codeword, int[] punctured, int errors, long seed)
    {
        ReedSolomon_63_ErasureDecoder decoder = new ReedSolomon_63_ErasureDecoder(35);
        int[] received = damage(codeword, punctured, errors, seed);
        int[] output = new int[63];
        boolean irrecoverable = decoder.decode(received, output, punctured);
        assertFalse(irrecoverable, "erasures=" + punctured.length + " errors=" + errors + " seed=" + seed);
        assertArrayEquals(codeword, output, "erasures=" + punctured.length + " errors=" + errors + " seed=" + seed);
    }

    private static void assertRejects(int[] codeword, int[] punctured, int errors, long seed)
    {
        ReedSolomon_63_ErasureDecoder decoder = new ReedSolomon_63_ErasureDecoder(35);
        int[] received = damage(codeword, punctured, errors, seed);
        int[] output = new int[63];
        assertTrue(decoder.decode(received, output, punctured),
            "should be uncorrectable: erasures=" + punctured.length + " errors=" + errors + " seed=" + seed);
    }

    @Test
    public void cleanCodewordDecodesAndReconstructsErasures()
    {
        for(long seed = 1; seed <= 20; seed++)
        {
            assertCorrects(codeword(seed, 9), FACCH_PUNCTURED, 0, seed);
            assertCorrects(codeword(seed, 5), SACCH_PUNCTURED, 0, seed);
        }
    }

    @Test
    public void facchCorrectsNineErrorsWithNinePuncturedParity()
    {
        //2 * errors + erasures <= 28: 9 erasures leave 9 correctable errors, not 5.
        for(long seed = 1; seed <= 50; seed++)
        {
            assertCorrects(codeword(seed, 9), FACCH_PUNCTURED, 9, seed);
        }
    }

    @Test
    public void facchRejectsTenErrors()
    {
        for(long seed = 1; seed <= 50; seed++)
        {
            assertRejects(codeword(seed, 9), FACCH_PUNCTURED, 10, seed);
        }
    }

    @Test
    public void sacchCorrectsElevenErrorsWithSixPuncturedParity()
    {
        for(long seed = 1; seed <= 50; seed++)
        {
            assertCorrects(codeword(seed, 5), SACCH_PUNCTURED, 11, seed);
        }
    }

    @Test
    public void sacchRejectsTwelveErrors()
    {
        for(long seed = 1; seed <= 50; seed++)
        {
            assertRejects(codeword(seed, 5), SACCH_PUNCTURED, 12, seed);
        }
    }

    @Test
    public void withoutErasuresMatchesErrorsOnlyDecoderUpToFourteenErrors()
    {
        //Same field and generator as the existing BerlekempMassey-based decoder: both must agree.
        ReedSolomon_63_35_29_P25 reference = new ReedSolomon_63_35_29_P25();
        ReedSolomon_63_ErasureDecoder decoder = new ReedSolomon_63_ErasureDecoder(35);

        for(long seed = 1; seed <= 30; seed++)
        {
            int[] codeword = codeword(seed, 0);

            for(int errors : new int[]{0, 1, 7, 14})
            {
                int[] received = damage(codeword, NONE, errors, seed);
                int[] expected = new int[63];
                int[] actual = new int[63];
                boolean referenceFailed = reference.decode(received.clone(), expected);
                boolean failed = decoder.decode(received, actual, NONE);
                assertFalse(referenceFailed, "reference decoder failed at " + errors + " errors");
                assertFalse(failed, "erasure decoder failed at " + errors + " errors");
                assertArrayEquals(codeword, actual);
                assertArrayEquals(expected, actual);
            }
        }
    }

    @Test
    public void tooManyErasuresIsRejected()
    {
        ReedSolomon_63_ErasureDecoder decoder = new ReedSolomon_63_ErasureDecoder(35);
        int[] erasures = new int[29];

        for(int i = 0; i < erasures.length; i++)
        {
            erasures[i] = i;
        }

        int[] output = new int[63];
        assertTrue(decoder.decode(codeword(1, 0), output, erasures));
    }

    @Test
    public void erasedPositionsMayHoldAnyValue()
    {
        int[] codeword = codeword(7, 9);
        int[] received = codeword.clone();

        for(int p : FACCH_PUNCTURED)
        {
            received[p] = 0x3F; //not zero: the decoder must ignore whatever is there
        }

        int[] output = new int[63];
        ReedSolomon_63_ErasureDecoder decoder = new ReedSolomon_63_ErasureDecoder(35);
        assertFalse(decoder.decode(received, output, FACCH_PUNCTURED));
        assertArrayEquals(codeword, output);
        assertEquals(codeword[0], output[0]);
    }
}
