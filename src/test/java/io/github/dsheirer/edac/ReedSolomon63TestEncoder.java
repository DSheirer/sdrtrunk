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

/**
 * Systematic RS(63,35) encoder over GF(2^6), x^6 + x + 1, for tests: the code the P25 Phase 2 SACCH and FACCH
 * carry (shortened and punctured on air).  Codewords are produced in the BerlekempMassey / ReedSolomon_63_ErasureDecoder
 * convention: index i is the coefficient of x^i, so parity occupies indices 0..27 and information 28..62 (with the
 * shortened information symbols at the top left zero).
 */
public final class ReedSolomon63TestEncoder
{
    private static final int N = 63, K = 35;
    private static final int[] EXP = new int[126];
    private static final int[] LOG = new int[64];
    private static final int[] GENERATOR; //coefficient i of x^i, degree 28, monic

    static
    {
        int x = 1;

        for(int i = 0; i < 63; i++)
        {
            EXP[i] = x;
            LOG[x] = i;
            int previous = x;
            x = (x << 1) & 0x3F;

            if((previous & 0x20) != 0)
            {
                x ^= 0x03;
            }
        }

        for(int i = 63; i < 126; i++)
        {
            EXP[i] = EXP[i - 63];
        }

        //g(x) = product of (x + alpha^j) for j = 1..28
        int[] g = {1};

        for(int j = 1; j <= N - K; j++)
        {
            int root = EXP[j];
            int[] next = new int[g.length + 1];

            for(int i = 0; i < g.length; i++)
            {
                next[i + 1] ^= g[i];
                next[i] ^= mul(g[i], root);
            }

            g = next;
        }

        GENERATOR = g;
    }

    private ReedSolomon63TestEncoder()
    {
    }

    private static int mul(int a, int b)
    {
        return (a == 0 || b == 0) ? 0 : EXP[LOG[a] + LOG[b]];
    }

    /**
     * Encodes 35 information symbols (info[0] is the highest-degree symbol, x^62) into a 63-symbol codeword in the
     * x^i-at-index-i convention.
     */
    public static int[] encode(int[] info)
    {
        if(info.length != K)
        {
            throw new IllegalArgumentException("35 information symbols required");
        }

        //Long division of info(x) * x^28 by g(x); remainder is the parity.
        int[] remainder = new int[N - K];

        for(int i = 0; i < K; i++)
        {
            int feedback = (info[i] & 0x3F) ^ remainder[N - K - 1];

            for(int j = N - K - 1; j > 0; j--)
            {
                remainder[j] = remainder[j - 1] ^ mul(feedback, GENERATOR[j]);
            }

            remainder[0] = mul(feedback, GENERATOR[0]);
        }

        int[] codeword = new int[N];

        for(int i = 0; i < K; i++)
        {
            codeword[N - 1 - i] = info[i] & 0x3F;
        }

        System.arraycopy(remainder, 0, codeword, 0, N - K);
        return codeword;
    }
}
