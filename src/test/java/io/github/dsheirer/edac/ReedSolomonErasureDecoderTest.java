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

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the Reed-Solomon errors-and-erasures decoder against the P25 RS(63,35,29) code.
 */
public class ReedSolomonErasureDecoderTest
{
    private static final int N = 63;
    private static final int K = 35;
    private static final int PARITY = N - K;
    private int[] randomCodeword(Random random)
    {
        int[] message = new int[K];

        for(int i = 0; i < K; i++)
        {
            message[i] = random.nextInt(64);
        }

        return ReedSolomonTestEncoder.encode(message);
    }

    /**
     * Corrupts the codeword with the specified number of errors and erasures at distinct random positions.
     * @return erasure positions
     */
    private int[] corrupt(int[] codeword, int errors, int erasures, Random random)
    {
        int[] positions = new int[N];

        for(int i = 0; i < N; i++)
        {
            positions[i] = i;
        }

        for(int i = N - 1; i > 0; i--)
        {
            int j = random.nextInt(i + 1);
            int t = positions[i];
            positions[i] = positions[j];
            positions[j] = t;
        }

        for(int i = 0; i < errors; i++)
        {
            codeword[positions[i]] ^= 1 + random.nextInt(63);
        }

        int[] erased = Arrays.copyOfRange(positions, errors, errors + erasures);

        for(int position : erased)
        {
            codeword[position] = random.nextInt(64);
        }

        return erased;
    }

    @Test
    public void encoderProducesValidCodewords()
    {
        Random random = new Random(1);
        BerlekempMassey reference = new ReedSolomon_63_35_29_P25();

        for(int trial = 0; trial < 100; trial++)
        {
            int[] codeword = randomCodeword(random);
            int[] output = new int[N];
            assertFalse(reference.decode(codeword, output));
            assertArrayEquals(codeword, output);
        }
    }

    @Test
    public void correctsErrorsAndErasuresWithinCapacity()
    {
        Random random = new Random(2);
        ReedSolomonErasureDecoder decoder = ReedSolomonErasureDecoder.p25_63_35_29();

        for(int erasures = 0; erasures <= PARITY; erasures++)
        {
            int maxErrors = (PARITY - erasures) / 2;

            for(int errors = 0; errors <= maxErrors; errors++)
            {
                for(int trial = 0; trial < 20; trial++)
                {
                    int[] codeword = randomCodeword(random);
                    int[] received = codeword.clone();
                    int[] erased = corrupt(received, errors, erasures, random);
                    int[] output = new int[N];
                    assertFalse(decoder.decode(received, output, erased, erased.length),
                            "Failed with errors [" + errors + "] erasures [" + erasures + "]");
                    assertArrayEquals(codeword, output, "Errors [" + errors + "] erasures [" + erasures + "]");
                }
            }
        }
    }

    @Test
    public void detectsMostDecodingFailuresBeyondCapacity()
    {
        Random random = new Random(3);
        ReedSolomonErasureDecoder decoder = ReedSolomonErasureDecoder.p25_63_35_29();
        int miscorrections = 0;
        int trials = 1000;

        for(int trial = 0; trial < trials; trial++)
        {
            int[] codeword = randomCodeword(random);
            int[] received = codeword.clone();
            int erasures = 6;
            int errors = (PARITY - erasures) / 2 + 3;
            int[] erased = corrupt(received, errors, erasures, random);
            int[] output = new int[N];

            if(!decoder.decode(received, output, erased, erased.length))
            {
                //A miscorrection must still produce a valid (but incorrect) codeword
                assertFalse(Arrays.equals(codeword, output));
                int[] check = new int[N];
                assertFalse(new ReedSolomon_63_35_29_P25().decode(output, check));
                miscorrections++;
            }
        }

        assertTrue(miscorrections < trials / 100, "Miscorrections: " + miscorrections);
    }

    @Test
    public void matchesErrorsOnlyDecoder()
    {
        Random random = new Random(4);
        ReedSolomonErasureDecoder decoder = ReedSolomonErasureDecoder.p25_63_35_29();
        BerlekempMassey reference = new ReedSolomon_63_35_29_P25();

        for(int trial = 0; trial < 500; trial++)
        {
            int[] received = randomCodeword(random);
            corrupt(received, random.nextInt(18), 0, random);
            int[] output = new int[N];
            int[] referenceOutput = new int[N];
            boolean failed = decoder.decode(received, output, new int[0], 0);
            boolean referenceFailed = reference.decode(received, referenceOutput);
            assertEquals(referenceFailed, failed);

            if(!failed)
            {
                assertArrayEquals(referenceOutput, output);
            }
        }
    }
}
