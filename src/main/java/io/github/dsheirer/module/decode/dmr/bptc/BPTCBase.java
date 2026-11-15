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

package io.github.dsheirer.module.decode.dmr.bptc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.ProductCodeDecoder;
import java.util.Arrays;

/**
 * Block Product Turbo Code (BPTC) base implementation for BPTC codes where each row is a Hamming codeword and each
 * column has even parity (ie the final row is the column parity row).  Bits are ordered row by row.
 *
 * Uses hard decision minimum distance decoding that corrects every error pattern up to half the product code minimum
 * distance and most patterns up to the maximum error count.  Optionally falls back to row erasure decoding when the
 * minimum distance decoder can't correct the message.
 */
public abstract class BPTCBase
{
    public static int ERRORS_NOT_CORRECTED = -2;
    private final ProductCodeDecoder mDecoder;
    private final int[] mRowChecks;
    private final int[] mRowErrorColumns;
    private final int mColumnCount;
    private final int mRowCount;
    private final int mFallbackMaximumErrors;

    /**
     * Constructs an instance
     * @param rowChecks parity check matrix column for each bit position of the row (Hamming) code, where the array
     * length is the column count.
     * @param rowCount number of rows, including the column parity row.
     * @param maximumErrors maximum number of bit errors to correct with minimum distance decoding.
     * @param fallbackMaximumErrors maximum number of bit errors to correct with row erasure decoding when the minimum
     * distance decoder can't correct the message, or 0 to disable the fallback.
     */
    public BPTCBase(int[] rowChecks, int rowCount, int maximumErrors, int fallbackMaximumErrors)
    {
        mRowChecks = rowChecks.clone();
        mColumnCount = rowChecks.length;
        mRowCount = rowCount;
        mFallbackMaximumErrors = fallbackMaximumErrors;
        int[] columnChecks = new int[rowCount];
        Arrays.fill(columnChecks, 1);
        mDecoder = new ProductCodeDecoder(rowChecks, columnChecks, maximumErrors);

        //Single bit error locator for the row code: column for each syndrome, or -1
        int combined = 0;

        for(int check: rowChecks)
        {
            combined |= check;
        }

        mRowErrorColumns = new int[Integer.highestOneBit(combined) << 1];
        Arrays.fill(mRowErrorColumns, -1);

        for(int column = 0; column < mColumnCount; column++)
        {
            mRowErrorColumns[rowChecks[column]] = column;
        }
    }

    /**
     * Performs error detection and correction.  When corrected, the corrected bit count is incremented by the number
     * of corrected bits.  Otherwise, the message is unchanged and the corrected bit count is set to ERRORS_NOT_CORRECTED.
     * @param message to correct
     * @return true if the message was corrected or false if there are uncorrectable errors.
     */
    public boolean correct(CorrectedBinaryMessage message)
    {
        int[] rows = new int[mRowCount];

        for(int row = 0; row < mRowCount; row++)
        {
            rows[row] = getRow(row, message);
        }

        int corrected = mDecoder.decode(rows);

        if(corrected == ProductCodeDecoder.UNCORRECTABLE && mFallbackMaximumErrors > 0)
        {
            corrected = correctRowErasure(rows);
        }

        if(corrected == ProductCodeDecoder.UNCORRECTABLE)
        {
            message.setCorrectedBitCount(ERRORS_NOT_CORRECTED);
            return false;
        }

        if(corrected > 0)
        {
            for(int row = 0; row < mRowCount; row++)
            {
                for(int column = 0; column < mColumnCount; column++)
                {
                    message.set(row * mColumnCount + column, (rows[row] & (1 << column)) != 0);
                }
            }

            message.incrementCorrectedBitCount(corrected);
        }

        return true;
    }

    /**
     * Row erasure decoding fallback for messages that the minimum distance decoder can't correct (more bit errors than
     * the maximum, or equidistant from more than one codeword).  Corrects single bit errors in each row and then
     * recomputes one row from the column parity of the other rows, choosing the row that produces the nearest
     * codeword.  Real-world messages can have bit errors that this corrects beyond the minimum distance decoder maximum.
     * @param rows to correct, unchanged when uncorrectable.
     * @return number of corrected bit errors, or UNCORRECTABLE.
     */
    private int correctRowErasure(int[] rows)
    {
        int[] decoded = rows.clone();
        int failedRow = -1;

        for(int row = 0; row < mRowCount; row++)
        {
            int syndrome = ProductCodeDecoder.getSyndrome(rows[row], mRowChecks);

            if(syndrome != 0)
            {
                int column = mRowErrorColumns[syndrome];

                if(column >= 0)
                {
                    decoded[row] ^= 1 << column;
                }
                else if(failedRow >= 0)
                {
                    return ProductCodeDecoder.UNCORRECTABLE; //More than one row with uncorrectable errors
                }
                else
                {
                    failedRow = row;
                }
            }
        }

        int[] best = null;
        int bestDistance = Integer.MAX_VALUE;
        boolean tie = false;

        for(int erased = 0; erased < mRowCount; erased++)
        {
            if(failedRow >= 0 && erased != failedRow)
            {
                continue;
            }

            int[] candidate = decoded.clone();
            int parity = 0;

            for(int row = 0; row < mRowCount; row++)
            {
                if(row != erased)
                {
                    parity ^= candidate[row];
                }
            }

            candidate[erased] = parity;
            int distance = 0;

            for(int row = 0; row < mRowCount; row++)
            {
                distance += Integer.bitCount(candidate[row] ^ rows[row]);
            }

            if(distance < bestDistance)
            {
                best = candidate;
                bestDistance = distance;
                tie = false;
            }
            else if(distance == bestDistance && !Arrays.equals(candidate, best))
            {
                tie = true;
            }
        }

        if(tie || bestDistance > mFallbackMaximumErrors)
        {
            return ProductCodeDecoder.UNCORRECTABLE;
        }

        System.arraycopy(best, 0, rows, 0, mRowCount);
        return bestDistance;
    }

    /**
     * Row word from the message, where bit i is the bit in column i.
     */
    private int getRow(int row, BinaryMessage message)
    {
        int word = 0;
        int offset = row * mColumnCount;

        for(int column = 0; column < mColumnCount; column++)
        {
            if(message.get(offset + column))
            {
                word |= 1 << column;
            }
        }

        return word;
    }

    /**
     * Calculates the row (Hamming) syndrome.
     * @param row to check
     * @param message containing bits to check
     * @return syndrome, zero when the row is a valid codeword.
     */
    private int getRowSyndrome(int row, BinaryMessage message)
    {
        return ProductCodeDecoder.getSyndrome(getRow(row, message), mRowChecks);
    }

    /**
     * Indicates if the column passes an even parity check.
     * @param column to test
     * @return true if the parity check passes
     */
    public boolean isColumnCorrect(int column, BinaryMessage message)
    {
        boolean correct = true;

        for(int row = 0; row < mRowCount; row++)
        {
            correct ^= message.get(row * mColumnCount + column);
        }

        return correct;
    }

    /**
     * Returns a bit map indicating the row errors.
     * @param message containing rows.
     * @return row error map
     */
    public BinaryMessage getRowErrors(CorrectedBinaryMessage message)
    {
        BinaryMessage rows = new BinaryMessage(mRowCount);

        for(int row = 0; row < mRowCount; row++)
        {
            rows.set(row, getRowSyndrome(row, message) != 0);
        }

        return rows;
    }

    /**
     * Returns a bit map indicating the column errors.
     * @param message containing rows.
     * @return column error map
     */
    public BinaryMessage getColumnErrors(CorrectedBinaryMessage message)
    {
        BinaryMessage columns = new BinaryMessage(mColumnCount);

        for(int column = 0; column < mColumnCount; column++)
        {
            columns.set(column, !isColumnCorrect(column, message));
        }

        return columns;
    }

    public void logErrorMap(CorrectedBinaryMessage message)
    {
        StringBuilder sb = new StringBuilder();
        for(int row = 0; row < mRowCount; row++)
        {
            int offset = row * mColumnCount;
            sb.append("Row ").append(row).append(": ");
            sb.append(message.getSubMessage(offset, offset + mColumnCount));
            sb.append(" (").append(offset).append(":").append(offset + mColumnCount).append(")");

            for(int error = offset; error < offset + mColumnCount; error++)
            {
                if(message.get(error))
                {
                    sb.append(" ").append(error);
                }
            }

            sb.append("\n");

        }

        System.out.println(sb);
    }

    public void logDiagnostic(CorrectedBinaryMessage message)
    {
        StringBuilder sb = new StringBuilder();
        for(int row = 0; row < mRowCount; row++)
        {
            int offset = row * mColumnCount;
            sb.append(" Row ").append(row).append(": ");
            sb.append(message.getSubMessage(offset, offset + mColumnCount));
            int syndrome = getRowSyndrome(row, message);
            if(syndrome != 0)
            {
                sb.append(" < Syndrome:0x").append(Integer.toHexString(syndrome).toUpperCase());
            }
            sb.append("\n");
        }

        sb.append("Errors: ");

        BinaryMessage columnErrors = getColumnErrors(message);
        for(int column = 0; column < mColumnCount; column++)
        {
            if(columnErrors.get(column))
            {
                sb.append("^");
            }
            else
            {
                sb.append(".");
            }
        }

        System.out.println("Diagnostic:\n" + sb + "\n");
    }
}
