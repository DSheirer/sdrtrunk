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

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Iterative soft decision decoder for product codes where each row is a codeword of a short block code and each
 * column has even parity (ie the final row is the column parity row).  Bits are ordered row by row.
 *
 * Alternately decodes the rows using Chase-Pyndiah decoding and the even parity columns using min-sum decoding,
 * exchanging extrinsic soft information, until the decisions form a valid product codeword.  Rejects decisions
 * where the correction cost (sum of the normalized channel soft bit magnitudes for the corrected bits) exceeds the
 * maximum, since a low correction cost indicates a reliable decision.
 */
public class SoftProductCodeDecoder
{
    private static final int CHASE_LEAST_RELIABLE_BITS = 4;
    //Each half iteration decodes either all rows or all columns, starting with the rows.  Alpha scales the extrinsic
    //information fed to the next half iteration and beta is the reliability assigned to row bits without a competing
    //candidate codeword.
    private static final float[] SOFT_ALPHA = new float[]{0.0f, 0.2f, 0.3f, 0.5f, 0.7f, 0.9f, 1.0f, 1.0f};
    private static final float[] SOFT_BETA = new float[]{0.2f, 0.4f, 0.6f, 0.8f, 1.0f, 1.0f, 1.0f, 1.0f};
    private final SoftBlockCode mRowCode;
    private final int mRowCount;
    private final int mColumnCount;
    private final int mLength;
    private final int[][] mRowIndexes;
    private final float mMaxCorrectionCost;

    /**
     * Constructs an instance
     * @param rowCode for each row, where the code length is the column count
     * @param rowCount number of rows, including the column parity row
     * @param maxCorrectionCost maximum correction cost for a decision, or Float.MAX_VALUE for no limit
     */
    public SoftProductCodeDecoder(SoftBlockCode rowCode, int rowCount, float maxCorrectionCost)
    {
        mRowCode = rowCode;
        mRowCount = rowCount;
        mColumnCount = rowCode.getLength();
        mLength = mRowCount * mColumnCount;
        mMaxCorrectionCost = maxCorrectionCost;
        mRowIndexes = new int[mRowCount][mColumnCount];

        for(int row = 0; row < mRowCount; row++)
        {
            for(int column = 0; column < mColumnCount; column++)
            {
                mRowIndexes[row][column] = row * mColumnCount + column;
            }
        }
    }

    /**
     * Decodes the (de-interleaved) soft bits.
     * @param channel soft bits ordered row by row where a positive value favors a 1 bit.
     * @return decoded message with the corrected bit count set, or null if decoding doesn't converge or the correction
     * cost exceeds the maximum.
     */
    public CorrectedBinaryMessage decode(float[] channel)
    {
        if(channel == null || channel.length != mLength)
        {
            return null;
        }

        //Normalize the channel soft bits to a mean magnitude of 1 for the alpha and beta settings
        float sum = 0.0f;

        for(float softBit: channel)
        {
            sum += Math.abs(softBit);
        }

        if(sum <= 0.0f)
        {
            return null;
        }

        float scale = mLength / sum;
        float[] received = new float[mLength];

        for(int i = 0; i < mLength; i++)
        {
            received[i] = channel[i] * scale;
        }

        float[] extrinsic = new float[mLength];
        float[] input = new float[mLength];
        float[] output = new float[mLength];

        for(int halfIteration = 0; halfIteration < SOFT_ALPHA.length; halfIteration++)
        {
            float alpha = SOFT_ALPHA[halfIteration];

            for(int i = 0; i < mLength; i++)
            {
                input[i] = received[i] + alpha * extrinsic[i];
            }

            CorrectedBinaryMessage decoded = new CorrectedBinaryMessage(mLength);

            if(halfIteration % 2 == 0)
            {
                boolean allDecoded = true;

                for(int row = 0; row < mRowCount; row++)
                {
                    int word = mRowCode.decodeChase(input, mRowIndexes[row], output, CHASE_LEAST_RELIABLE_BITS,
                            SOFT_BETA[halfIteration]);
                    allDecoded &= word >= 0;

                    for(int column = 0; column < mColumnCount; column++)
                    {
                        if((word & (1 << column)) != 0)
                        {
                            decoded.set(mRowIndexes[row][column]);
                        }
                    }
                }

                if(!allDecoded)
                {
                    decoded = null;
                }
            }
            else
            {
                decodeColumns(input, output);

                for(int i = 0; i < mLength; i++)
                {
                    if(input[i] + output[i] > 0)
                    {
                        decoded.set(i);
                    }
                }
            }

            //Check for a valid product codeword from the decisions of this half iteration
            if(decoded != null && isValid(decoded))
            {
                int corrected = 0;
                float cost = 0.0f;

                for(int i = 0; i < mLength; i++)
                {
                    if(decoded.get(i) != (received[i] > 0))
                    {
                        corrected++;
                        cost += Math.abs(received[i]);
                    }
                }

                if(cost > mMaxCorrectionCost)
                {
                    return null;
                }

                decoded.setCorrectedBitCount(corrected);
                return decoded;
            }

            //Normalize the extrinsic information to a mean magnitude of 1 for the next half iteration
            float extrinsicSum = 0.0f;

            for(float value: output)
            {
                extrinsicSum += Math.abs(value);
            }

            float extrinsicScale = extrinsicSum > 0.0f ? mLength / extrinsicSum : 0.0f;

            for(int i = 0; i < mLength; i++)
            {
                extrinsic[i] = output[i] * extrinsicScale;
            }
        }

        return null;
    }

    /**
     * Indicates if each row is a row codeword and each column has even parity.
     */
    private boolean isValid(CorrectedBinaryMessage message)
    {
        int parity = 0;

        for(int row = 0; row < mRowCount; row++)
        {
            int word = 0;

            for(int column = 0; column < mColumnCount; column++)
            {
                if(message.get(mRowIndexes[row][column]))
                {
                    word |= (1 << column);
                }
            }

            if(!mRowCode.isCodeword(word))
            {
                return false;
            }

            parity ^= word;
        }

        return parity == 0;
    }

    /**
     * Min-sum (max-log) soft-in/soft-out decoding of the even parity columns.  The extrinsic value for each bit is
     * the product of the signs and the minimum magnitude of the other bits in the column.
     * @param input soft bits
     * @param output to receive the extrinsic values.
     */
    private void decodeColumns(float[] input, float[] output)
    {
        for(int column = 0; column < mColumnCount; column++)
        {
            float sign = 1.0f;
            float minimum1 = Float.MAX_VALUE;
            float minimum2 = Float.MAX_VALUE;
            int minimumRow = -1;

            for(int row = 0; row < mRowCount; row++)
            {
                float value = input[mRowIndexes[row][column]];

                if(value < 0)
                {
                    sign = -sign;
                }

                float magnitude = Math.abs(value);

                if(magnitude < minimum1)
                {
                    minimum2 = minimum1;
                    minimum1 = magnitude;
                    minimumRow = row;
                }
                else if(magnitude < minimum2)
                {
                    minimum2 = magnitude;
                }
            }

            for(int row = 0; row < mRowCount; row++)
            {
                int index = mRowIndexes[row][column];
                float value = input[index];
                float otherSign = value < 0 ? -sign : sign;
                output[index] = otherSign * (row == minimumRow ? minimum2 : minimum1);
            }
        }
    }
}
