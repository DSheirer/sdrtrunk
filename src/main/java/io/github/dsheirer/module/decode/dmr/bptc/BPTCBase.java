/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
import io.github.dsheirer.edac.IHamming;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Block Product Turbo Code (BPTC) base implementation for BPTC codes using simple even parity column checks.
 */
public abstract class BPTCBase
{
    public static int ERRORS_NOT_CORRECTED = -2;
    private IHamming mHamming;
    private int mColumnCount;
    private int mRowCount;

    /**
     * Constructs an instance
     * @param hamming to provide row checking using the correct Hamming error detection and correction algorithm.
     * @param column count
     * @param row count
     */
    public BPTCBase(IHamming hamming, int columnCount, int rowCount)
    {
        mHamming = hamming;
        mColumnCount = columnCount;
        mRowCount = rowCount;
    }

    /**
     * Used by the correct() method to employ an optional attempt to correct multiple row, two-bit errors where each
     * set of row errors don't shadow each other based on a test where the column cardinality is twice the row
     * cardinality.  This is probably not mathematically correct and should only be enabled when the decoded message
     * payload has a good CRC check after the extraction (ie. only use for FLC).
     * @return true if enabled, false (default) otherwise
     */
    protected boolean canCorrectMultiRow2BitErrors()
    {
        return false;
    }

    /**
     * Performs error detection and correction.
     * @param message to correct
     * @return true if the message was corrected or false if there are uncorrectable errors.
     */
    public boolean correct(CorrectedBinaryMessage message)
    {
        BinaryMessage columns = getColumnErrors(message);
        BinaryMessage rows = getRowErrors(message);

        if(columns.isEmpty() && rows.isEmpty())
        {
            return true;
        }

        List<Integer> correctedIndexes = new ArrayList<>();

        //1: fix non-shadowed single bit errors in each row
        for(int row = rows.nextSetBit(0); row >= 0 && row < mRowCount; row = rows.nextSetBit(row + 1))
        {
            int index = correct1BitErrors(row, message, rows);

            if(index >= 0)
            {
                correctedIndexes.add(index);
            }
        }

        //Refresh the column errors
        columns = getColumnErrors(message);

        if(columns.isEmpty() && rows.isEmpty())
        {
            message.incrementCorrectedBitCount(correctedIndexes.size());
            return true;
        }

        //2: fix non-shadowed multi bit errors (up to 2) in each row
        if(columns.cardinality() <= 2)
        {
            for(int row = rows.nextSetBit(0); row >= 0 && row < mRowCount; row = rows.nextSetBit(row + 1))
            {
                List<Integer> solution = correctMultiBitErrors(row, message, columns, rows);

                if(solution.size() > 0)
                {
                    correctedIndexes.addAll(solution);
                }
            }
        }

        //3: fix single row, multi-column errors
        if(rows.cardinality() == 1 && !columns.isEmpty())
        {
            List<Integer> solution = correctMultiBitErrors(rows.nextSetBit(0), message, columns, rows);

            if(!solution.isEmpty())
            {
                correctedIndexes.addAll(solution);
            }
        }

        //4: fix one or more rows that each have 2 bit errors, not shadowing each other.
        if(canCorrectMultiRow2BitErrors() && rows.cardinality() > 0 && columns.cardinality() / rows.cardinality() == 2)
        {
            List<Integer> solution = correctMultipleRowTwoBitErrorsNotShadowing(columns, rows, message);

            if(!solution.isEmpty())
            {
                correctedIndexes.addAll(solution);
            }
        }

        if(columns.isEmpty() && rows.isEmpty())
        {
            message.incrementCorrectedBitCount(correctedIndexes.size());
            return true;
        }

        message.setCorrectedBitCount(ERRORS_NOT_CORRECTED);
        return false;
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
     * Calculates the column from the bit index/offset
     * @param index for a bit
     * @return column that contains the index
     */
    public int getColumn(int index)
    {
        return index % mColumnCount;
    }

    /**
     * Calculates the index for a bit that exists at the row and column.
     * @param column for the bit.
     * @param row for the bit
     * @return index for the bit.
     */
    public int getIndex(int column, int row)
    {
        return row * mColumnCount + column;
    }

    /**
     * Checks the row for bit errors.
     * @param row to check
     * @param message containing bits to check
     * @return true if the row passes the Hamming check or false if not.
     */
    private boolean isRowCorrect(int row, CorrectedBinaryMessage message)
    {
        return mHamming.getErrorIndex(message, row * mColumnCount) == IHamming.NO_ERRORS;
    }

    /**
     * Calculates the index for the bit error in the message
     * @param row to check
     * @param message with bits
     * @return error index in the range 0-195, or -1 if the message row has no bit errors.
     */
    private int getRowErrorIndex(int row, CorrectedBinaryMessage message)
    {
        return mHamming.getErrorIndex(message, row * mColumnCount);
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
            rows.set(row, !isRowCorrect(row, message));
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

    /**
     * Calculates the indices that make up the intersections of the row and column errors.
     * @param rows bitmap indicating rows with errors
     * @param columns bitmap indicating columns with errors
     * @return set of intersection indices.
     */
    public List<Integer> getIntersectionIndices(BinaryMessage columns, BinaryMessage rows)
    {
        List<Integer> intersections = new ArrayList<>();

        for(int row = rows.nextSetBit(0); row >= 0 && row < mRowCount; row = rows.nextSetBit(row + 1))
        {
            for(int column = columns.nextSetBit(0); column >= 0 && column < mColumnCount; column = columns.nextSetBit(column + 1))
            {
                intersections.add(getIndex(column, row));
            }
        }

        return intersections;
    }

    /**
     * Calculates the indices that make up the intersections of the row and column errors.
     * @param rows bitmap indicating rows with errors
     * @param columns bitmap indicating columns with errors
     * @return set of intersection indices.
     */
    public List<Integer> getIntersectionIndices(BinaryMessage columns, int row)
    {
        List<Integer> intersections = new ArrayList<>();

        for(int column = columns.nextSetBit(0); column >= 0 && column < mColumnCount; column = columns.nextSetBit(column + 1))
        {
            intersections.add(getIndex(column, row));
        }

        return intersections;
    }

    /**
     * Correct multiple rows with two-bit errors each where the error columns are not shadowing each other.
     * @param columns error map
     * @param rows error map
     * @param message to correct
     * @return corrected indices
     */
    public List<Integer> correctMultipleRowTwoBitErrorsNotShadowing(BinaryMessage columns, BinaryMessage rows,
                                                                    CorrectedBinaryMessage message)
    {
        List<Integer> solution = new ArrayList<>();

        for(int row = rows.nextSetBit(0); row >= 0 && row < mRowCount; row = rows.nextSetBit(row + 1))
        {
            for(int column1 = columns.nextSetBit(0); column1 >= 0; column1 = columns.nextSetBit(column1 + 1))
            {
                for(int column2 = columns.nextSetBit(column1 + 1); column2 >= 0; column2 = columns.nextSetBit(column2 + 1))
                {
                    int index1 = getIndex(column1, row);
                    int index2 = getIndex(column2, row);
                    message.flip(index1);
                    message.flip(index2);

                    if(isRowCorrect(row, message))
                    {
                        solution.add(index1);
                        solution.add(index2);
                        columns.clear(column1);
                        columns.clear(column2);
                        rows.clear(row);
                        column1 = mColumnCount;
                        column2 = mColumnCount;
                    }
                    else
                    {
                        message.flip(index1);
                        message.flip(index2);
                    }
                }
            }
        }

        return solution;
    }

    /**
     * Corrects single bit errors in each row using only the Hamming error index.
     * @param row to correct
     * @param message with bits
     * @param rows error flag set
     * @return bit index that was corrected or -2 if the error was not corrected
     */
    public int correct1BitErrors(int row, CorrectedBinaryMessage message, BinaryMessage rows)
    {
        int index = getRowErrorIndex(row, message);

        if(index >= 0 && index < IHamming.MULTIPLE_ERRORS)
        {
            message.flip(index);

            if(isRowCorrect(row, message))
            {
                rows.clear(row);
                return index;
            }

            message.flip(index);
        }

        return ERRORS_NOT_CORRECTED;
    }

    /**
     * Corrects multiple bit errors in a single row when all other rows are corrected first, by flipping the
     * bits in the row that show errors in the columns and testing the solution.
     * @param row to correct
     * @param message with bits
     * @param columns bit error matrix
     * @param rows bit error matrix
     * @return solution or an empty list if no solution.
     */
    public List<Integer> correctMultiBitErrors(int row, CorrectedBinaryMessage message, BinaryMessage columns,
                                               BinaryMessage rows)
    {
        //Test flipping bits in the row everywhere that we have a column error
        List<Integer> testFlips = new ArrayList<>();

        for(int column = columns.nextSetBit(0); column >= 0 && column < mColumnCount;
            column = columns.nextSetBit(column + 1))
        {
            int toFlip = getIndex(column, row);
            message.flip(toFlip);
            testFlips.add(toFlip);
        }

        if(!isRowCorrect(row, message))
        {
            int index = getRowErrorIndex(row, message);

            if(index >= 0 && index < IHamming.MULTIPLE_ERRORS)
            {
                message.flip(index);

                if(testFlips.contains(index))
                {
                    testFlips.removeIf(value -> value == index);
                }
                else
                {
                    testFlips.add(index);
                }
            }
        }

        if(isRowCorrect(row, message))
        {
            //Check the columns also

            boolean columnsCorrect = true;

            for(Integer flip: testFlips)
            {
                columnsCorrect &= isColumnCorrect(getColumn(flip), message);
            }

            if(columnsCorrect)
            {
                rows.clear(row);

                for(Integer flip: testFlips)
                {
                    columns.clear(getColumn(flip));
                }

                return testFlips;
            }
        }

        for(Integer flip: testFlips)
        {
            message.flip(flip);
        }

        return Collections.emptyList();
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
            int index = getRowErrorIndex(row, message);
            if(index != IHamming.NO_ERRORS)
            {
                sb.append(" < Index:").append(index);

                if(index == IHamming.MULTIPLE_ERRORS)
                {
                    sb.append(" (2+ bit errors)");
                }
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
