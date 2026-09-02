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
 * Errors-and-erasures decoder for the P25 Reed-Solomon codes over GF(2^6) with primitive polynomial x^6 + x + 1
 * (RS(63,35,29) and its shortened forms).
 *
 * The P25 Phase 2 SACCH and FACCH carry a shortened AND punctured RS(63,35) codeword: the leading information
 * symbols are shortened away (known to be zero) and the trailing parity symbols are simply not transmitted.  An
 * errors-only decoder that is handed zeros in the punctured positions sees them as symbol errors and spends its
 * correction budget on them - 9 of 14 correctable symbols for FACCH, 6 of 14 for SACCH.  Marking the punctured
 * positions as erasures costs one unit of budget each instead of two (2 * errors + erasures <= 28), leaving 9 and
 * 11 correctable symbol errors respectively.
 *
 * Errors-and-erasures decoding via Forney-modified syndromes, Berlekamp-Massey, Chien search and the Forney
 * algorithm.  Adapted from the Apache-2.0 licensed GopherTrunk project (internal/radio/framing/rs_gf64.go).
 *
 * Symbol ordering follows the existing BerlekempMassey convention used by the P25 message parsers: input[i] is the
 * coefficient of x^i, so the parity symbols occupy the low indices and the (shortened) information symbols the
 * high indices.
 */
public class ReedSolomon_63_ErasureDecoder
{
    private static final int N = 63;
    private static final int[] EXP = new int[126];
    private static final int[] LOG = new int[64];
    private final int mParityCount;

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
                x ^= 0x03; //x^6 = x + 1
            }
        }

        for(int i = 63; i < 126; i++)
        {
            EXP[i] = EXP[i - 63];
        }
    }

    /**
     * Constructs a decoder for RS(63, k)
     * @param k information symbol count (35 for the P25 Phase 2 SACCH/FACCH code)
     */
    public ReedSolomon_63_ErasureDecoder(int k)
    {
        mParityCount = N - k;
    }

    private static int mul(int a, int b)
    {
        return (a == 0 || b == 0) ? 0 : EXP[LOG[a] + LOG[b]];
    }

    private static int pow(int n)
    {
        n %= 63;

        if(n < 0)
        {
            n += 63;
        }

        return EXP[n];
    }

    private static int inv(int a)
    {
        return EXP[63 - LOG[a]];
    }

    /**
     * Syndromes of the codeword, cw[0] holding the highest-degree coefficient.
     */
    private int[] syndromes(int[] cw)
    {
        int[] s = new int[mParityCount];

        for(int j = 1; j <= mParityCount; j++)
        {
            int alphaJ = pow(j);
            int acc = 0;

            for(int i = 0; i < N; i++)
            {
                acc = mul(acc, alphaJ) ^ cw[i];
            }

            s[j - 1] = acc;
        }

        return s;
    }

    private static int[] polyAddShiftScaled(int[] a, int[] b, int shift, int coefficient)
    {
        int[] out = new int[Math.max(a.length, b.length + shift)];
        System.arraycopy(a, 0, out, 0, a.length);

        for(int i = 0; i < b.length; i++)
        {
            out[i + shift] ^= mul(coefficient, b[i]);
        }

        return out;
    }

    private static int polyEval(int[] poly, int x)
    {
        int acc = 0;
        int xp = 1;

        for(int c : poly)
        {
            acc ^= mul(c, xp);
            xp = mul(xp, x);
        }

        return acc;
    }

    private static int[] polyMulMod(int[] a, int[] b, int degree)
    {
        int[] out = new int[degree];

        for(int i = 0; i < a.length && i < degree; i++)
        {
            if(a[i] == 0)
            {
                continue;
            }

            for(int j = 0; j < b.length && i + j < degree; j++)
            {
                out[i + j] ^= mul(a[i], b[j]);
            }
        }

        return out;
    }

    private static int[] formalDerivative(int[] poly)
    {
        if(poly.length <= 1)
        {
            return new int[]{0};
        }

        int[] out = new int[poly.length - 1];

        for(int i = 1; i < poly.length; i += 2)
        {
            out[i - 1] = poly[i];
        }

        return out;
    }

    /**
     * Berlekamp-Massey.  Returns the error locator polynomial; its degree is returned via degree[0].
     */
    private static int[] berlekampMassey(int[] syndromes, int[] degree)
    {
        int[] lambda = {1};
        int[] bPoly = {1};
        int l = 0;
        int m = 1;
        int b = 1;

        for(int i = 0; i < syndromes.length; i++)
        {
            int delta = syndromes[i];

            for(int j = 1; j <= l && j < lambda.length; j++)
            {
                delta ^= mul(lambda[j], syndromes[i - j]);
            }

            if(delta == 0)
            {
                m++;
            }
            else if(2 * l <= i)
            {
                int[] previous = lambda.clone();
                lambda = polyAddShiftScaled(lambda, bPoly, m, mul(delta, inv(b)));
                l = i + 1 - l;
                bPoly = previous;
                b = delta;
                m = 1;
            }
            else
            {
                lambda = polyAddShiftScaled(lambda, bPoly, m, mul(delta, inv(b)));
                m++;
            }
        }

        degree[0] = l;
        return lambda;
    }

    /**
     * Decodes the codeword.
     *
     * @param input codeword, input[i] being the coefficient of x^i (BerlekempMassey convention). Erased and
     * shortened positions may hold any value.
     * @param output receives the corrected codeword in the same convention, or a copy of the input when the
     * codeword is not correctable.
     * @param erasurePositions indices into input of the symbols that were not received (punctured parity).
     * @return true when the codeword could NOT be corrected (matches BerlekempMassey.decode()'s return convention).
     */
    public boolean decode(int[] input, int[] output, int[] erasurePositions)
    {
        int erasureCount = erasurePositions.length;

        if(erasureCount > mParityCount)
        {
            System.arraycopy(input, 0, output, 0, N);
            return true;
        }

        //Reverse into highest-degree-first order and blank the erasures
        int[] cw = new int[N];

        for(int i = 0; i < N; i++)
        {
            cw[i] = input[N - 1 - i] & 0x3F;
        }

        for(int e : erasurePositions)
        {
            cw[N - 1 - e] = 0;
        }

        int[] synd = syndromes(cw);

        //Erasure locator: product of (1 + Y x) over the erasure locators Y = alpha^(N-1-index)
        int[] gamma = {1};

        for(int e : erasurePositions)
        {
            int y = pow(N - 1 - (N - 1 - e));
            int[] next = new int[gamma.length + 1];

            for(int i = 0; i < gamma.length; i++)
            {
                next[i] ^= gamma[i];
                next[i + 1] ^= mul(gamma[i], y);
            }

            gamma = next;
        }

        //Forney syndromes: the first erasureCount coefficients of S(x) * Gamma(x) are fixed by the erasures
        int[] product = polyMulMod(synd, gamma, mParityCount);
        int[] forney = new int[mParityCount - erasureCount];
        System.arraycopy(product, erasureCount, forney, 0, forney.length);

        int[] degree = new int[1];
        int[] lambda = berlekampMassey(forney, degree);
        int errorCount = degree[0];

        if(2 * errorCount > mParityCount - erasureCount)
        {
            System.arraycopy(input, 0, output, 0, N);
            return true;
        }

        //Errata locator: roots are the erasures and the errors together
        int[] sigma = polyMulMod(lambda, gamma, lambda.length + gamma.length);
        int[] positions = new int[N];
        int[] locators = new int[N];
        int found = 0;

        for(int p = 0; p < N; p++)
        {
            if(polyEval(sigma, pow(-p)) == 0)
            {
                positions[found] = N - 1 - p;
                locators[found] = pow(p);
                found++;
            }
        }

        if(found != errorCount + erasureCount)
        {
            System.arraycopy(input, 0, output, 0, N);
            return true;
        }

        int[] omega = polyMulMod(synd, sigma, mParityCount);
        int[] sigmaPrime = formalDerivative(sigma);

        for(int i = 0; i < found; i++)
        {
            int xInverse = inv(locators[i]);
            int denominator = polyEval(sigmaPrime, xInverse);

            if(denominator == 0)
            {
                System.arraycopy(input, 0, output, 0, N);
                return true;
            }

            cw[positions[i]] ^= mul(polyEval(omega, xInverse), inv(denominator));
        }

        for(int s : syndromes(cw))
        {
            if(s != 0)
            {
                System.arraycopy(input, 0, output, 0, N);
                return true;
            }
        }

        for(int i = 0; i < N; i++)
        {
            output[N - 1 - i] = cw[i];
        }

        return false;
    }
}
