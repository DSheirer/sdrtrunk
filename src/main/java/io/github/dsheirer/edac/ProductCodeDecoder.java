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

/**
 * Hard decision minimum distance decoder for binary product codes, where each row is a codeword of a short row code
 * and each column is a codeword of a systematic column code with the column parity bits in the final rows.
 *
 * Finds the minimum weight error pattern that produces the received row and column syndromes, using an iterative
 * deepening search over the error patterns (coset) of each information row syndrome.  The column parity row errors
 * are fully determined by the column syndromes and the information row error choices, and the column parity rows then
 * also match their row syndromes, so only the information rows are searched.
 *
 * Corrects every error pattern up to half the product code minimum distance, and most patterns up to the maximum
 * error count.  When more than one error pattern has the minimum weight, the received word is equidistant from more
 * than one codeword and is treated as uncorrectable.
 *
 * Row words are integers where bit i (LSB = 0) holds the bit in column i.  Instances are thread safe.
 */
public class ProductCodeDecoder
{
    public static final int UNCORRECTABLE = -1;
    //Bounds the decode time for pathological inputs (~20 ms).  Error injection testing with random error patterns of
    //each weight: the largest search was 630k nodes (3 ms) for BPTC(196,96) with a maximum error count of 12.
    private static final long MAXIMUM_SEARCH_NODES = 4_000_000;
    private final int mColumnCount;
    private final int mRowCount;
    private final int mInformationRowCount;
    private final int[] mRowChecks;
    private final int[] mColumnChecks;
    private final int mColumnCheckBits;
    private final long mColumnGroupMask;
    private final int mMaximumErrors;
    private final int[][] mCosets;
    private final int[] mMinimumWeights;

    /**
     * Constructs an instance.
     * @param rowChecks parity check matrix column (syndrome of a single bit error) for each row code bit position, where
     * the array length is the column count (maximum 20).
     * @param columnChecks parity check matrix column for each column code bit position, where the array length is the
     * row count.  The column code must be systematic, with distinct single bit parity checks for the final (parity)
     * rows.  For a single (even) parity check column code, each value is 1.
     * @param maximumErrors maximum number of bit errors to correct.
     */
    public ProductCodeDecoder(int[] rowChecks, int[] columnChecks, int maximumErrors)
    {
        mColumnCount = rowChecks.length;
        mRowCount = columnChecks.length;
        mRowChecks = rowChecks.clone();
        mColumnChecks = columnChecks.clone();
        mMaximumErrors = maximumErrors;
        mColumnCheckBits = bitLength(mColumnChecks);
        mInformationRowCount = mRowCount - mColumnCheckBits;

        if(mColumnCount < 1 || mColumnCount > 20)
        {
            throw new IllegalArgumentException("Column count must be in range 1 - 20");
        }

        if(mColumnCount * mColumnCheckBits > Long.SIZE || mInformationRowCount < 1)
        {
            throw new IllegalArgumentException("Unsupported column code dimensions");
        }

        //Verify the final rows are the systematic parity rows
        int parityChecks = 0;

        for(int row = mInformationRowCount; row < mRowCount; row++)
        {
            int check = mColumnChecks[row];

            if(Integer.bitCount(check) != 1 || (parityChecks & check) != 0)
            {
                throw new IllegalArgumentException("Column code parity rows must have distinct single bit checks");
            }

            parityChecks |= check;
        }

        long groupMask = 0;

        for(int column = 0; column < mColumnCount; column++)
        {
            groupMask |= 1L << (column * mColumnCheckBits);
        }

        mColumnGroupMask = groupMask;

        //Coset lists: the error patterns for each row syndrome, up to the maximum error count, ordered by weight
        int syndromeCount = 1 << bitLength(mRowChecks);
        int maximumWeight = Math.min(maximumErrors, mColumnCount);
        int wordCount = 1 << mColumnCount;
        int[] syndromes = new int[wordCount];
        int[] sizes = new int[syndromeCount];

        for(int word = 0; word < wordCount; word++)
        {
            syndromes[word] = getSyndrome(word, mRowChecks);

            if(Integer.bitCount(word) <= maximumWeight)
            {
                sizes[syndromes[word]]++;
            }
        }

        mCosets = new int[syndromeCount][];
        mMinimumWeights = new int[syndromeCount];

        for(int syndrome = 0; syndrome < syndromeCount; syndrome++)
        {
            mCosets[syndrome] = new int[sizes[syndrome]];
        }

        int[] filled = new int[syndromeCount];

        for(int weight = 0; weight <= maximumWeight; weight++)
        {
            for(int word = 0; word < wordCount; word++)
            {
                if(Integer.bitCount(word) == weight)
                {
                    int syndrome = syndromes[word];
                    mCosets[syndrome][filled[syndrome]++] = word;
                }
            }
        }

        for(int syndrome = 0; syndrome < syndromeCount; syndrome++)
        {
            mMinimumWeights[syndrome] = mCosets[syndrome].length > 0 ? Integer.bitCount(mCosets[syndrome][0]) :
                    maximumWeight + 1;
        }
    }

    /**
     * Number of bits needed to represent the largest value.
     */
    private static int bitLength(int[] values)
    {
        int combined = 0;

        for(int value: values)
        {
            combined |= value;
        }

        return Integer.SIZE - Integer.numberOfLeadingZeros(combined);
    }

    /**
     * Calculates the parity check matrix columns for a code from a syndrome calculator, as the syndrome of each single
     * bit error.
     * @param length of the code in bits
     * @param syndrome calculator for a binary message of the code length.
     * @return parity check matrix column for each bit position.
     */
    public static int[] getParityChecks(int length, ToIntFunction<BinaryMessage> syndrome)
    {
        int[] checks = new int[length];

        for(int i = 0; i < length; i++)
        {
            BinaryMessage message = new BinaryMessage(length);
            message.set(i);
            checks[i] = syndrome.applyAsInt(message);
        }

        return checks;
    }

    /**
     * Calculates the syndrome of the word.
     * @param word where bit i (LSB = 0) is code bit position i
     * @param checks parity check matrix column for each bit position
     * @return syndrome, zero for a valid codeword.
     */
    public static int getSyndrome(int word, int[] checks)
    {
        int syndrome = 0;

        while(word != 0)
        {
            syndrome ^= checks[Integer.numberOfTrailingZeros(word)];
            word &= word - 1;
        }

        return syndrome;
    }

    /**
     * Indicates if all rows and columns are valid codewords.
     * @param rows to check
     * @return true if valid.
     */
    public boolean isValid(int[] rows)
    {
        long columnSyndromes = 0;

        for(int row = 0; row < mRowCount; row++)
        {
            if(getSyndrome(rows[row], mRowChecks) != 0)
            {
                return false;
            }

            columnSyndromes ^= getColumnContribution(row, rows[row]);
        }

        return columnSyndromes == 0;
    }

    /**
     * Corrects the rows to the nearest valid product codeword when the number of bit errors doesn't exceed the
     * maximum error count and the nearest codeword is unique.
     * @param rows to correct, where bit i of each row is the bit in column i.  Unchanged when uncorrectable.
     * @return number of corrected bit errors, or UNCORRECTABLE.
     */
    public int decode(int[] rows)
    {
        if(rows.length != mRowCount)
        {
            throw new IllegalArgumentException("Expected " + mRowCount + " rows");
        }

        Search search = new Search();
        boolean rowErrors = false;

        for(int row = 0; row < mRowCount; row++)
        {
            search.mRowSyndromes[row] = getSyndrome(rows[row], mRowChecks);
            rowErrors |= search.mRowSyndromes[row] != 0;
            search.mColumnSyndromes ^= getColumnContribution(row, rows[row]);
        }

        if(!rowErrors && search.mColumnSyndromes == 0)
        {
            return 0;
        }

        //Lower bound on the error count of the remaining rows, including the column parity rows, at each search depth
        int lowerBound = 0;

        for(int row = mInformationRowCount; row < mRowCount; row++)
        {
            lowerBound += mMinimumWeights[search.mRowSyndromes[row]];
        }

        search.mLowerBounds[mInformationRowCount] = lowerBound;

        for(int row = mInformationRowCount - 1; row >= 0; row--)
        {
            search.mLowerBounds[row] = search.mLowerBounds[row + 1] + mMinimumWeights[search.mRowSyndromes[row]];
        }

        int start = Math.max(search.mLowerBounds[0], getErrorColumnCount(search.mColumnSyndromes));

        for(search.mBudget = start; search.mBudget <= mMaximumErrors; search.mBudget++)
        {
            search(search, 0, 0, 0L);

            if(search.mAborted || search.mSolutionCount > 1)
            {
                return UNCORRECTABLE;
            }

            if(search.mSolutionCount == 1)
            {
                for(int row = 0; row < mInformationRowCount; row++)
                {
                    rows[row] ^= search.mSolution[row];
                }

                long residual = search.mColumnSyndromes ^ search.mSolutionContribution;

                for(int row = mInformationRowCount; row < mRowCount; row++)
                {
                    int check = mColumnChecks[row];

                    for(int column = 0; column < mColumnCount; column++)
                    {
                        if(((residual >>> (column * mColumnCheckBits)) & check) != 0)
                        {
                            rows[row] ^= 1 << column;
                        }
                    }
                }

                return search.mBudget;
            }
        }

        return UNCORRECTABLE;
    }

    /**
     * Depth first search over the information row error patterns, for solutions that don't exceed the error budget.
     * @param search state
     * @param row to choose an error pattern for
     * @param used error count of the error patterns chosen for the preceding rows
     * @param contribution to the column syndromes from the error patterns chosen for the preceding rows
     */
    private void search(Search search, int row, int used, long contribution)
    {
        if(++search.mNodes > MAXIMUM_SEARCH_NODES)
        {
            search.mAborted = true;
            return;
        }

        //The remaining column syndromes must be resolved by the remaining rows
        long residual = search.mColumnSyndromes ^ contribution;

        if(row == mInformationRowCount)
        {
            //Each remaining column syndrome bit is a column parity row error
            if(used + Long.bitCount(residual) <= search.mBudget && ++search.mSolutionCount == 1)
            {
                System.arraycopy(search.mChosen, 0, search.mSolution, 0, mInformationRowCount);
                search.mSolutionContribution = contribution;
            }

            return;
        }

        if(used + Math.max(search.mLowerBounds[row], getErrorColumnCount(residual)) > search.mBudget)
        {
            return;
        }

        int remainingLowerBound = search.mLowerBounds[row + 1];

        for(int error: mCosets[search.mRowSyndromes[row]])
        {
            int weight = Integer.bitCount(error);

            if(used + weight + remainingLowerBound > search.mBudget)
            {
                break; //Error patterns are ordered by weight
            }

            search.mChosen[row] = error;
            search(search, row + 1, used + weight, contribution ^ getColumnContribution(row, error));

            if(search.mAborted || search.mSolutionCount > 1)
            {
                return;
            }
        }
    }

    /**
     * Calculates the contribution of a row word to the column syndromes, where the syndrome for each column occupies
     * a group of column check bits, starting with column 0 in the LSBs.
     */
    private long getColumnContribution(int row, int word)
    {
        long contribution = 0;
        long check = mColumnChecks[row];

        while(word != 0)
        {
            contribution ^= check << (Integer.numberOfTrailingZeros(word) * mColumnCheckBits);
            word &= word - 1;
        }

        return contribution;
    }

    /**
     * Number of columns with a non-zero syndrome, where each requires at least one more bit error.
     */
    private int getErrorColumnCount(long columnSyndromes)
    {
        long folded = columnSyndromes;

        for(int shift = 1; shift < mColumnCheckBits; shift++)
        {
            folded |= columnSyndromes >>> shift;
        }

        return Long.bitCount(folded & mColumnGroupMask);
    }

    /**
     * Search state for a single decode.
     */
    private final class Search
    {
        private final int[] mRowSyndromes = new int[mRowCount];
        private final int[] mLowerBounds = new int[mInformationRowCount + 1];
        private final int[] mChosen = new int[mInformationRowCount];
        private final int[] mSolution = new int[mInformationRowCount];
        private long mColumnSyndromes;
        private long mSolutionContribution;
        private int mBudget;
        private int mSolutionCount;
        private long mNodes;
        private boolean mAborted;
    }
}
