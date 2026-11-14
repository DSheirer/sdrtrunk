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

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * EDACS message voter. Performs majority voting across 3 copies of a 40-bit EDACS
 * control channel word (normal, inverted, normal) and validates BCH(40,28) parity.
 * Reference: DSD-FME edacs-fme.c and edacs-bch3.c.
 */
public class EDACSVoter
{
    private static final int CODEWORD_BITS = 40;
    private static final int DATA_BITS = 28;
    private static final int PARITY_BITS = 12;
    private static final long CODEWORD_MASK = 0xFFFFFFFFFFL;
    private static final int[] GENERATOR_POLYNOMIAL = createGeneratorPolynomial();

    /**
     * Votes across 3 copies of an EDACS message word. Copies 1 and 3 are normal polarity,
     * copy 2 is inverted.
     *
     * @param copy1 first copy (normal)
     * @param copy2 second copy (inverted)
     * @param copy3 third copy (normal)
     * @return corrected 28-bit data message, or null if uncorrectable
     */
    public CorrectedBinaryMessage vote(CorrectedBinaryMessage copy1, CorrectedBinaryMessage copy2, CorrectedBinaryMessage copy3)
    {
        long voted = 0;

        for(int x = 0; x < CODEWORD_BITS; x++)
        {
            int count = 0;
            if(copy1.get(x)) count++;
            if(!copy2.get(x)) count++; //inverted copy
            if(copy3.get(x)) count++;

            if(count >= 2)
            {
                voted |= (1L << (CODEWORD_BITS - 1 - x));
            }
        }

        long data = voted >> PARITY_BITS;
        long regenerated = encode(data);

        if((voted & CODEWORD_MASK) != regenerated)
        {
            return null;
        }

        CorrectedBinaryMessage message = new CorrectedBinaryMessage(DATA_BITS);

        for(int x = 0; x < DATA_BITS; x++)
        {
            if(((data >> (DATA_BITS - 1 - x)) & 1L) == 1L)
            {
                message.set(x);
            }
        }

        message.setCorrectedBitCount(0);
        return message;
    }

    static long encode(long message)
    {
        int[] data = new int[DATA_BITS];
        int[] parity = new int[PARITY_BITS];

        for(int x = 0; x < DATA_BITS; x++)
        {
            data[x] = (int)((message >> x) & 0x1);
        }

        for(int x = DATA_BITS - 1; x >= 0; x--)
        {
            int feedback = data[x] ^ parity[PARITY_BITS - 1];

            if(feedback != 0)
            {
                for(int y = PARITY_BITS - 1; y > 0; y--)
                {
                    parity[y] = GENERATOR_POLYNOMIAL[y] != 0 ? parity[y - 1] ^ feedback : parity[y - 1];
                }

                parity[0] = GENERATOR_POLYNOMIAL[0] != 0 ? feedback : 0;
            }
            else
            {
                for(int y = PARITY_BITS - 1; y > 0; y--)
                {
                    parity[y] = parity[y - 1];
                }

                parity[0] = 0;
            }
        }

        long codeword = 0;

        for(int x = CODEWORD_BITS - 1; x >= 0; x--)
        {
            int bit = x < PARITY_BITS ? parity[x] : data[x - PARITY_BITS];
            codeword = (codeword << 1) | bit;
        }

        return codeword & CODEWORD_MASK;
    }

    private static int[] createGeneratorPolynomial()
    {
        int m = 6;
        int n = 63;
        int t = 2;
        int d = 2 * t + 1;
        int[] primitive = new int[m + 1];
        primitive[0] = 1;
        primitive[1] = 1;
        primitive[m] = 1;

        int[] alphaTo = new int[n + 1];
        int[] indexOf = new int[n + 1];
        int mask = 1;
        alphaTo[m] = 0;

        for(int x = 0; x < m; x++)
        {
            alphaTo[x] = mask;
            indexOf[alphaTo[x]] = x;

            if(primitive[x] != 0)
            {
                alphaTo[m] ^= mask;
            }

            mask <<= 1;
        }

        indexOf[alphaTo[m]] = m;
        mask >>= 1;

        for(int x = m + 1; x < n; x++)
        {
            if(alphaTo[x - 1] >= mask)
            {
                alphaTo[x] = alphaTo[m] ^ ((alphaTo[x - 1] ^ mask) << 1);
            }
            else
            {
                alphaTo[x] = alphaTo[x - 1] << 1;
            }

            indexOf[alphaTo[x]] = x;
        }

        indexOf[0] = -1;

        int[][] cycle = new int[1024][21];
        int[] size = new int[1024];
        int[] min = new int[1024];
        int[] zeros = new int[1024];
        cycle[0][0] = 0;
        size[0] = 1;
        cycle[1][0] = 1;
        size[1] = 1;
        int cycleIndex = 1;
        int candidate;

        do
        {
            int position = 0;
            do
            {
                position++;
                cycle[cycleIndex][position] = (cycle[cycleIndex][position - 1] * 2) % n;
                size[cycleIndex]++;
                candidate = (cycle[cycleIndex][position] * 2) % n;
            }
            while(candidate != cycle[cycleIndex][0]);

            int next = 0;
            boolean seen;
            do
            {
                next++;
                seen = false;

                for(int x = 1; x <= cycleIndex && !seen; x++)
                {
                    for(int y = 0; y < size[x] && !seen; y++)
                    {
                        seen = next == cycle[x][y];
                    }
                }
            }
            while(seen && next < (n - 1));

            if(!seen)
            {
                cycleIndex++;
                cycle[cycleIndex][0] = next;
                size[cycleIndex] = 1;
            }

            candidate = next;
        }
        while(candidate < (n - 1));

        int terms = 0;
        int redundancy = 0;

        for(int x = 1; x <= cycleIndex; x++)
        {
            boolean found = false;

            for(int y = 0; y < size[x] && !found; y++)
            {
                for(int root = 1; root < d && !found; root++)
                {
                    if(root == cycle[x][y])
                    {
                        found = true;
                        min[terms] = x;
                    }
                }
            }

            if(min[terms] != 0)
            {
                redundancy += size[min[terms]];
                terms++;
            }
        }

        int zeroIndex = 1;
        for(int x = 0; x < terms; x++)
        {
            for(int y = 0; y < size[min[x]]; y++)
            {
                zeros[zeroIndex++] = cycle[min[x]][y];
            }
        }

        int[] generator = new int[redundancy + 1];
        generator[0] = alphaTo[zeros[1]];
        generator[1] = 1;

        for(int x = 2; x <= redundancy; x++)
        {
            generator[x] = 1;

            for(int y = x - 1; y > 0; y--)
            {
                if(generator[y] != 0)
                {
                    generator[y] = generator[y - 1] ^ alphaTo[(indexOf[generator[y]] + zeros[x]) % n];
                }
                else
                {
                    generator[y] = generator[y - 1];
                }
            }

            generator[0] = alphaTo[(indexOf[generator[0]] + zeros[x]) % n];
        }

        return generator;
    }
}
