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

package io.github.dsheirer.edac.soft;

import java.util.Arrays;
import java.util.function.IntPredicate;

/**
 * Soft decision decoder for short binary block codes, defined by the full set of valid codewords.
 *
 * Codewords are represented as integers where bit i (LSB = 0) holds codeword bit position i.  Soft bit values are
 * log-likelihood ratios where a positive value favors a 1 bit and the magnitude indicates the reliability.
 *
 * Provides Chase-Pyndiah soft-in/soft-out decoding: tests the codewords nearest to the hard decision with the least
 * reliable bits flipped and produces extrinsic soft information for iterative (ie product code) decoding.
 */
public class SoftBlockCode
{
    private static final int MAX_LENGTH = 17;
    private final int mLength;
    private final int[] mCodewords;
    private final int[] mDecodeTable;

    /**
     * Constructs an instance.
     * @param length of the code in bits (maximum 17).
     * @param codewords set of all valid codewords.
     */
    public SoftBlockCode(int length, int[] codewords)
    {
        if(length < 1 || length > MAX_LENGTH)
        {
            throw new IllegalArgumentException("Code length must be in range 1 - " + MAX_LENGTH);
        }

        mLength = length;
        mCodewords = codewords.clone();

        //Single bit error correction table: maps each word within distance 1 of a codeword to that codeword, or -1.
        mDecodeTable = new int[1 << length];
        Arrays.fill(mDecodeTable, -1);

        for(int codeword: codewords)
        {
            mDecodeTable[codeword] = codeword;

            for(int i = 0; i < length; i++)
            {
                mDecodeTable[codeword ^ (1 << i)] = codeword;
            }
        }
    }

    /**
     * Creates an instance by testing every possible word of the specified length to identify the valid codewords.
     * @param length of the code in bits (maximum 17).
     * @param isCodeword test that indicates if the word (bit i = codeword position i) is a valid codeword.
     * @return soft block code.
     */
    public static SoftBlockCode fromCodewordTest(int length, IntPredicate isCodeword)
    {
        int count = 0;
        int[] candidates = new int[1 << length];

        for(int word = 0; word < (1 << length); word++)
        {
            if(isCodeword.test(word))
            {
                candidates[count++] = word;
            }
        }

        return new SoftBlockCode(length, Arrays.copyOf(candidates, count));
    }

    /**
     * Code length in bits
     */
    public int getLength()
    {
        return mLength;
    }

    /**
     * Valid codewords
     */
    public int[] getCodewords()
    {
        return mCodewords;
    }

    /**
     * Indicates if the word is a valid codeword.
     */
    public boolean isCodeword(int word)
    {
        return mDecodeTable[word] == word;
    }

    /**
     * Hard decision of the soft bits.
     * @param softBits array
     * @param indices of the code bits in the soft bits array
     * @return hard decision word.
     */
    public static int getHardDecision(float[] softBits, int[] indices)
    {
        int word = 0;

        for(int i = 0; i < indices.length; i++)
        {
            if(softBits[indices[i]] > 0)
            {
                word |= (1 << i);
            }
        }

        return word;
    }

    /**
     * Calculates the reliability metric for the candidate codeword: the sum of the soft bit magnitudes for each bit
     * where the candidate differs from the hard decision.  Lower values are more likely.
     */
    private static float getMetric(int candidate, int hard, float[] magnitudes)
    {
        float metric = 0.0f;
        int difference = candidate ^ hard;

        while(difference != 0)
        {
            int bit = Integer.numberOfTrailingZeros(difference);
            metric += magnitudes[bit];
            difference &= difference - 1;
        }

        return metric;
    }

    /**
     * Chase-Pyndiah soft-in/soft-out decode.  Generates test patterns by flipping each combination of the least
     * reliable bits in the hard decision, decodes each with the single bit error correction table and selects the
     * most likely candidate codeword as the decision.  For each bit, the extrinsic value is the metric difference
     * between the decision and the best competing candidate that differs at that bit, less the soft input, or beta
     * (signed by the decision) when there is no competing candidate.  When no test pattern decodes to a codeword, the
     * extrinsic values are zero.
     *
     * @param input soft bits array
     * @param indices of the code bits in the input and output arrays
     * @param output array to receive the extrinsic soft values at the code bit indices
     * @param leastReliableCount number of least reliable bits to use for test patterns
     * @param beta reliability to assign to a bit when there is no competing candidate codeword.
     * @return decision codeword, or -1 if no test pattern decoded to a codeword.
     */
    public int decodeChase(float[] input, int[] indices, float[] output, int leastReliableCount, float beta)
    {
        float[] magnitudes = new float[mLength];

        for(int i = 0; i < mLength; i++)
        {
            magnitudes[i] = Math.abs(input[indices[i]]);
        }

        int hard = getHardDecision(input, indices);

        //Identify the least reliable bit positions using selection sort over the small code length
        int p = Math.min(leastReliableCount, mLength);
        int[] leastReliable = new int[p];
        int used = 0;

        for(int x = 0; x < p; x++)
        {
            int minIndex = -1;

            for(int i = 0; i < mLength; i++)
            {
                if((used & (1 << i)) == 0 && (minIndex < 0 || magnitudes[i] < magnitudes[minIndex]))
                {
                    minIndex = i;
                }
            }

            leastReliable[x] = minIndex;
            used |= (1 << minIndex);
        }

        //Decode each test pattern into a unique candidate codeword
        int[] candidates = new int[1 << p];
        float[] metrics = new float[1 << p];
        int candidateCount = 0;
        int decision = -1;
        float decisionMetric = Float.MAX_VALUE;

        for(int pattern = 0; pattern < (1 << p); pattern++)
        {
            int test = hard;

            for(int b = 0; b < p; b++)
            {
                if((pattern & (1 << b)) != 0)
                {
                    test ^= (1 << leastReliable[b]);
                }
            }

            int candidate = mDecodeTable[test];

            if(candidate < 0)
            {
                continue;
            }

            boolean duplicate = false;

            for(int c = 0; c < candidateCount; c++)
            {
                if(candidates[c] == candidate)
                {
                    duplicate = true;
                    break;
                }
            }

            if(!duplicate)
            {
                float metric = getMetric(candidate, hard, magnitudes);
                candidates[candidateCount] = candidate;
                metrics[candidateCount] = metric;
                candidateCount++;

                if(metric < decisionMetric)
                {
                    decisionMetric = metric;
                    decision = candidate;
                }
            }
        }

        for(int i = 0; i < mLength; i++)
        {
            if(decision < 0)
            {
                output[indices[i]] = 0.0f;
                continue;
            }

            int mask = 1 << i;
            float sign = (decision & mask) != 0 ? 1.0f : -1.0f;
            float competitorMetric = Float.MAX_VALUE;

            for(int c = 0; c < candidateCount; c++)
            {
                if(((candidates[c] ^ decision) & mask) != 0 && metrics[c] < competitorMetric)
                {
                    competitorMetric = metrics[c];
                }
            }

            if(competitorMetric < Float.MAX_VALUE)
            {
                output[indices[i]] = sign * (competitorMetric - decisionMetric) - input[indices[i]];
            }
            else
            {
                output[indices[i]] = sign * beta;
            }
        }

        return decision;
    }
}
