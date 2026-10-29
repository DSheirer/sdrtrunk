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
 * Reed-Solomon errors-and-erasures decoder.  Corrects any combination of v errors and e erasures (symbols at known
 * positions with unknown values) where 2v + e <= n - k.  Erasures support decoding of punctured codes (ie the punctured
 * parity symbols are erasures instead of errors) and soft decision (ie generalized minimum distance) decoding where the
 * least reliable received symbols are erased.
 *
 * Uses the same code conventions as the BerlekempMassey decoder: symbols are in polynomial form, symbol index j is the
 * coefficient of x^j, the generator polynomial roots are alpha^1 through alpha^(n-k), and the code is systematic with the
 * parity symbols at indices 0 to (n-k-1) and the message symbols at indices (n-k) to (n-1).
 *
 * Algorithm: computes the syndromes, the erasure locator polynomial and the modified (Forney) syndromes, applies the
 * Berlekamp-Massey algorithm to the modified syndromes to find the error locator polynomial, combines the error and
 * erasure locators into the errata locator, then uses a Chien search to find the errata locations and the Forney
 * algorithm to calculate the errata values.  The corrected codeword is verified to have zero syndromes.
 */
public class ReedSolomonErasureDecoder
{
    private final int mN;
    private final int mParityCount;
    private final int mFieldSize;
    private final int[] mExp;
    private final int[] mLog;

    /**
     * Constructs an instance
     * @param bitSize of each symbol (ie 6 for GF(2^6))
     * @param n codeword length in symbols
     * @param k message length in symbols
     * @param generatorPolynomial for the Galois field in big-endian form (see BerlekempMassey)
     */
    public ReedSolomonErasureDecoder(int bitSize, int n, int k, int[] generatorPolynomial)
    {
        mN = n;
        mParityCount = n - k;
        mFieldSize = (1 << bitSize) - 1;
        mExp = new int[mFieldSize * 2];
        mLog = new int[mFieldSize + 1];

        int primitive = 0;

        for(int i = 0; i < bitSize; i++)
        {
            if(generatorPolynomial[i] != 0)
            {
                primitive |= (1 << i);
            }
        }

        int value = 1;

        for(int i = 0; i < mFieldSize; i++)
        {
            mExp[i] = value;
            mExp[i + mFieldSize] = value;
            mLog[value] = i;
            value <<= 1;

            if(value > mFieldSize)
            {
                value = (value & mFieldSize) ^ primitive;
            }
        }
    }

    /**
     * APCO-25 RS(63,35,29) decoder used for P25 Phase 2 FACCH, SACCH and LCCH signaling.
     */
    public static ReedSolomonErasureDecoder p25_63_35_29()
    {
        return new ReedSolomonErasureDecoder(6, 63, 35, ReedSolomon_63_P25.P25_GENERATOR_POLYNOMIAL);
    }

    /**
     * Maximum number of erasures (with zero errors) that can be corrected.
     */
    public int getParityCount()
    {
        return mParityCount;
    }

    private int multiply(int a, int b)
    {
        if(a == 0 || b == 0)
        {
            return 0;
        }

        return mExp[mLog[a] + mLog[b]];
    }

    private int divide(int a, int b)
    {
        if(a == 0)
        {
            return 0;
        }

        return mExp[mLog[a] - mLog[b] + mFieldSize];
    }

    /**
     * Alpha raised to the power, for any non-negative power.
     */
    private int alpha(int power)
    {
        return mExp[power % mFieldSize];
    }

    /**
     * Evaluates the polynomial (coefficients in ascending order) at x.
     */
    private int evaluate(int[] polynomial, int length, int x)
    {
        int result = 0;

        for(int i = length - 1; i >= 0; i--)
        {
            result = multiply(result, x) ^ polynomial[i];
        }

        return result;
    }

    /**
     * Calculates the syndromes S1 through S(n-k) into syndromes[0 .. n-k-1].
     * @return true if any syndrome is non-zero.
     */
    private boolean syndromes(int[] codeword, int[] syndromes)
    {
        boolean error = false;

        for(int i = 0; i < mParityCount; i++)
        {
            int s = 0;
            int x = alpha(i + 1);

            for(int j = mN - 1; j >= 0; j--)
            {
                s = multiply(s, x) ^ codeword[j];
            }

            syndromes[i] = s;
            error |= (s != 0);
        }

        return error;
    }

    /**
     * Decodes the received codeword with the specified erasure positions.
     *
     * @param input received codeword symbols (polynomial form) of length n.  Values at the erasure positions are ignored.
     * @param output receives the corrected codeword, or a copy of the input (with zeros at the erasure positions) when
     * the codeword can't be corrected.
     * @param erasures positions of the erased symbols
     * @param erasureCount number of erasure positions to use from the erasures array
     * @return true if the codeword has irrecoverable errors, consistent with the BerlekempMassey decoder.
     */
    public boolean decode(int[] input, int[] output, int[] erasures, int erasureCount)
    {
        for(int j = 0; j < mN; j++)
        {
            output[j] = input[j];
        }

        for(int x = 0; x < erasureCount; x++)
        {
            output[erasures[x]] = 0;
        }

        if(erasureCount > mParityCount)
        {
            return true;
        }

        int[] syndromes = new int[mParityCount];

        if(!syndromes(output, syndromes))
        {
            return false;
        }

        int length = mParityCount + 1;

        //Erasure locator: product of (1 + X x) for each erasure location X = alpha^position
        int[] erasureLocator = new int[length];
        erasureLocator[0] = 1;

        for(int x = 0; x < erasureCount; x++)
        {
            int location = alpha(erasures[x]);

            for(int i = x + 1; i > 0; i--)
            {
                erasureLocator[i] ^= multiply(erasureLocator[i - 1], location);
            }
        }

        //Modified (Forney) syndromes: erasure locator x syndrome polynomial, modulo x^(n-k)
        int[] modified = new int[mParityCount];

        for(int i = 0; i < mParityCount; i++)
        {
            int value = 0;

            for(int j = 0; j <= Math.min(i, erasureCount); j++)
            {
                value ^= multiply(erasureLocator[j], syndromes[i - j]);
            }

            modified[i] = value;
        }

        //Berlekamp-Massey on the modified syndromes beyond the erasure count to find the error locator
        int[] errorLocator = new int[length];
        int[] previous = new int[length];
        int[] temp = new int[length];
        errorLocator[0] = 1;
        previous[0] = 1;
        int errorCount = 0;
        int shift = 1;
        int previousDiscrepancy = 1;
        int sequenceLength = mParityCount - erasureCount;

        for(int n = 0; n < sequenceLength; n++)
        {
            int discrepancy = modified[erasureCount + n];

            for(int i = 1; i <= errorCount; i++)
            {
                discrepancy ^= multiply(errorLocator[i], modified[erasureCount + n - i]);
            }

            if(discrepancy == 0)
            {
                shift++;
            }
            else
            {
                int scale = divide(discrepancy, previousDiscrepancy);
                System.arraycopy(errorLocator, 0, temp, 0, length);

                for(int i = 0; i + shift < length; i++)
                {
                    errorLocator[i + shift] ^= multiply(scale, previous[i]);
                }

                if(2 * errorCount <= n)
                {
                    errorCount = n + 1 - errorCount;
                    System.arraycopy(temp, 0, previous, 0, length);
                    previousDiscrepancy = discrepancy;
                    shift = 1;
                }
                else
                {
                    shift++;
                }
            }
        }

        if(2 * errorCount + erasureCount > mParityCount)
        {
            return true;
        }

        //Errata locator: error locator x erasure locator
        int errataDegree = errorCount + erasureCount;
        int[] errataLocator = new int[length];

        for(int i = 0; i <= errorCount; i++)
        {
            if(errorLocator[i] != 0)
            {
                for(int j = 0; j <= erasureCount; j++)
                {
                    errataLocator[i + j] ^= multiply(errorLocator[i], erasureLocator[j]);
                }
            }
        }

        //Errata evaluator: syndrome polynomial x errata locator, modulo x^(n-k)
        int[] evaluator = new int[mParityCount];

        for(int i = 0; i < mParityCount; i++)
        {
            int value = 0;

            for(int j = 0; j <= Math.min(i, errataDegree); j++)
            {
                value ^= multiply(errataLocator[j], syndromes[i - j]);
            }

            evaluator[i] = value;
        }

        //Formal derivative of the errata locator (odd power terms only in GF(2^m))
        int[] derivative = new int[length];

        for(int i = 1; i <= errataDegree; i += 2)
        {
            derivative[i - 1] = errataLocator[i];
        }

        //Chien search for the errata locations, with the Forney algorithm for the errata values
        int rootCount = 0;

        for(int j = 0; j < mN; j++)
        {
            int inverse = alpha(mFieldSize - (j % mFieldSize));

            if(evaluate(errataLocator, errataDegree + 1, inverse) == 0)
            {
                rootCount++;
                int denominator = evaluate(derivative, errataDegree, inverse);

                if(denominator == 0)
                {
                    restore(input, output, erasures, erasureCount);
                    return true;
                }

                output[j] ^= divide(evaluate(evaluator, mParityCount, inverse), denominator);
            }
        }

        if(rootCount != errataDegree || syndromes(output, syndromes))
        {
            restore(input, output, erasures, erasureCount);
            return true;
        }

        return false;
    }

    /**
     * Restores the output to the received codeword after a decoding failure.
     */
    private void restore(int[] input, int[] output, int[] erasures, int erasureCount)
    {
        for(int j = 0; j < mN; j++)
        {
            output[j] = input[j];
        }

        for(int x = 0; x < erasureCount; x++)
        {
            output[erasures[x]] = 0;
        }
    }
}
