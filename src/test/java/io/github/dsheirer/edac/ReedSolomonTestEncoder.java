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
 * Systematic encoder for the P25 RS(63,35,29) code (and codes shortened from it) for unit tests.  Parity symbols are
 * at indices 0-27 and message symbols at indices 28-62, with generator polynomial roots alpha^1 through alpha^28.
 */
public class ReedSolomonTestEncoder
{
    private static final int N = 63;
    private static final int K = 35;
    private static final int PARITY = N - K;
    private static final int[] EXP = new int[63];
    private static final int[] LOG = new int[64];

    static
    {
        int value = 1;

        for(int i = 0; i < 63; i++)
        {
            EXP[i] = value;
            LOG[value] = i;
            value <<= 1;

            if(value > 63)
            {
                value = (value & 63) ^ 3; //x^6 = x + 1
            }
        }
    }

    private static int multiply(int a, int b)
    {
        return (a == 0 || b == 0) ? 0 : EXP[(LOG[a] + LOG[b]) % 63];
    }

    /**
     * Encodes the message symbols.
     * @param message symbols (35), where trailing zero symbols represent a shortened code
     * @return codeword (63 symbols)
     */
    public static int[] encode(int[] message)
    {
        int[] generator = new int[PARITY + 1];
        generator[0] = 1;

        for(int i = 1; i <= PARITY; i++)
        {
            int root = EXP[i % 63];

            for(int j = i; j > 0; j--)
            {
                generator[j] = generator[j - 1] ^ multiply(generator[j], root);
            }

            generator[0] = multiply(generator[0], root);
        }

        //Remainder of x^28 * m(x) divided by g(x)
        int[] remainder = new int[N];
        System.arraycopy(message, 0, remainder, PARITY, K);

        for(int i = N - 1; i >= PARITY; i--)
        {
            int coefficient = remainder[i];

            if(coefficient != 0)
            {
                for(int j = 0; j <= PARITY; j++)
                {
                    remainder[i - PARITY + j] ^= multiply(generator[j], coefficient);
                }
            }
        }

        int[] codeword = new int[N];
        System.arraycopy(remainder, 0, codeword, 0, PARITY);
        System.arraycopy(message, 0, codeword, PARITY, K);
        return codeword;
    }
}
