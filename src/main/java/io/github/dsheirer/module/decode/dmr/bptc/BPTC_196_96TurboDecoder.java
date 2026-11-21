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
import io.github.dsheirer.edac.Hamming13;
import io.github.dsheirer.edac.Hamming15;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Turbo (iterative path search) hard decision decoder for the de-interleaved BPTC(196,96) message.  Used as a fallback
 * for error patterns that the minimum distance decoder can't correct.  Real-world messages can have many bit errors
 * spread across the rows and columns, which this decoder can often correct by following paths of row and column
 * Hamming error indices that resolve each other.  Can also detect and often correct shadowing where the bit errors
 * occur in a 2x2, 2x3, 3x2 or 3x3 column/row grid.
 *
 * The search effort is bounded.  The message is modified even when it can't be corrected and the decoder can report
 * success for an invalid codeword, so the caller must use a copy of the message and verify the result.
 *
 * Instances are not thread safe.  Use a new instance for each message.
 */
class BPTC_196_96TurboDecoder
{
    //Maximum recursive search calls per message.  Testing with real-world (CRC verified) messages that exceeded the
    //minimum distance decoder error count: 29 of 30 needed fewer than 1,500 calls, whereas random noise needed a median
    //of 2,700 and up to 12 million calls.  This limit bounds random noise decoding to under 1 ms.
    private static final int MAXIMUM_SEARCH_CALLS = 3000;
    private static final int MAXIMUM_RECURSION_DEPTH = 20;
    private static final int ROW_COUNT = 13;
    private static final int COLUMN_COUNT = 15;
    private int mSearchCalls;

    /**
     * Performs turbo decoding of the de-interleaved message.
     * @param message to correct, modified even when uncorrectable.
     * @return true if the decoder reports the message as corrected.
     */
    public boolean correct(CorrectedBinaryMessage message)
    {
        return correct(message, true);
    }

    /**
     * Turbo decode implementation.
     * @param message to correct.
     * @param pursueShadows to test intersection flipping for double/triple shadows.  Shadows are only pursued once.
     * @return true if the message was corrected, false otherwise.
     */
    private boolean correct(CorrectedBinaryMessage message, boolean pursueShadows)
    {
        List<Integer> columnErrors = getColumnErrors(message);
        List<Integer> rowErrors = getRowErrors(message);

        if(columnErrors.isEmpty() && rowErrors.isEmpty())
        {
            return true;
        }

        //Fix the easy bits first, where the row and column error indices agree
        for(Integer index: intersection(columnErrors, rowErrors))
        {
            message.flip(index);
        }

        //Set of flags for columns with errors
        BinaryMessage columns = new BinaryMessage(COLUMN_COUNT);
        for(int column = 0; column < COLUMN_COUNT; column++)
        {
            columns.set(column, !isColumnCorrect(column, message));
        }

        //Set of flags for rows with errors
        BinaryMessage rows = new BinaryMessage(ROW_COUNT);
        for(int row = 0; row < ROW_COUNT; row++)
        {
            rows.set(row, !isRowCorrect(row, message));
        }

        if(columns.isEmpty() && rows.isEmpty())
        {
            return true; //No (more) errors
        }

        //1: fix up to 2-bit errors per column or row without pursuing multi-column or multi-row paths
        boolean pursue = false;

        for(int column = columns.nextSetBit(0); column >= 0 && column < COLUMN_COUNT;
            column = columns.nextSetBit(column + 1))
        {
            correctColumn(column, message, columns, rows, 0, pursue, new ArrayList<>());
        }

        if(!columns.isEmpty() || !rows.isEmpty())
        {
            for(int row = rows.nextSetBit(0); row >= 0 && row < ROW_COUNT; row = rows.nextSetBit(row + 1))
            {
                correctRow(row, message, columns, rows, 0, pursue, new ArrayList<>());
            }
        }

        //2: pursue multi-column and multi-row paths
        if(!columns.isEmpty() || !rows.isEmpty())
        {
            pursue = true;

            for(int column = columns.nextSetBit(0); column >= 0 && column < COLUMN_COUNT;
                column = columns.nextSetBit(column + 1))
            {
                correctColumn(column, message, columns, rows, 0, pursue, new ArrayList<>());
            }

            if(!columns.isEmpty() || !rows.isEmpty())
            {
                for(int row = rows.nextSetBit(0); row >= 0 && row < ROW_COUNT; row = rows.nextSetBit(row + 1))
                {
                    correctRow(row, message, columns, rows, 0, pursue, new ArrayList<>());
                }
            }
        }

        //3: Detect double/triple row/column bit errors that are shadowing each other and preventing the Hamming codes
        //from giving accurate bit error locations.  Flip all of their intersections and then attempt to correct the
        //message again, once.
        if(pursueShadows && ((columns.cardinality() == 2 || columns.cardinality() == 3) &&
                (rows.cardinality() == 2 || rows.cardinality() == 3)))
        {
            List<Integer> intersectionIndices = getIntersectionIndices(columns, rows);

            for(Integer intersectionIndex: intersectionIndices)
            {
                message.flip(intersectionIndex);
            }

            if(correct(message, false))
            {
                columns.clear();
                rows.clear();
            }
            else
            {
                for(Integer intersectionIndex: intersectionIndices)
                {
                    message.flip(intersectionIndex);
                }
            }
        }

        return columns.isEmpty() && rows.isEmpty();
    }

    /**
     * Recursively tests flipping the bit indicated by the column error index, followed by the bit indicated by the
     * resulting row error index, and so on, until the path resolves the row and column errors.
     * @return solution path, or an empty list.
     */
    private List<Integer> correctColumn(int column, CorrectedBinaryMessage message, BinaryMessage columns,
                                        BinaryMessage rows, int depth, boolean pursue, List<Integer> path)
    {
        if(++mSearchCalls > MAXIMUM_SEARCH_CALLS || depth > MAXIMUM_RECURSION_DEPTH)
        {
            return Collections.emptyList();
        }

        int index = Hamming13.getErrorIndex(message, BPTC_196_96.COLUMN_INDEXES[column]);
        int row = getRow(index);

        //If the candidate index is in range and the row for the candidate has an error, proceed
        if(0 <= index && index <= 195 && rows.get(row) && !path.contains(index))
        {
            message.flip(index);

            if(isColumnCorrect(column, message))
            {
                path.add(index);

                //If the row is now correct, stop here.
                if(isRowCorrect(row, message))
                {
                    columns.clear(column);
                    rows.clear(row);
                    return path;
                }

                //Recursively test bit flipping in the row.  A non-empty list is the solution set.
                List<Integer> solution = correctRow(row, message, columns, rows, depth + 1, pursue, path);

                if(!solution.isEmpty())
                {
                    columns.clear(column);
                    return solution;
                }
                else if(pursue) //Pursue additional column flips
                {
                    for(int column2 = columns.nextSetBit(column + 1); column2 >= 0 && column2 < COLUMN_COUNT;
                        column2 = columns.nextSetBit(column2 + 1))
                    {
                        List<Integer> solution2 = correctColumn(column2, message, columns, rows, depth + 1, pursue,
                                path);

                        if(!solution2.isEmpty())
                        {
                            columns.clear(column);
                            rows.clear(row);
                            return solution2;
                        }
                    }
                }

                path.removeLast();
            }

            //Revert the change
            message.flip(index);
        }

        return Collections.emptyList();
    }

    /**
     * Recursively tests flipping the bit indicated by the row error index, followed by the bit indicated by the
     * resulting column error index, and so on, until the path resolves the row and column errors.
     * @return solution path, or an empty list.
     */
    private List<Integer> correctRow(int row, CorrectedBinaryMessage message, BinaryMessage columns,
                                     BinaryMessage rows, int depth, boolean pursue, List<Integer> path)
    {
        if(++mSearchCalls > MAXIMUM_SEARCH_CALLS || depth > MAXIMUM_RECURSION_DEPTH)
        {
            return Collections.emptyList();
        }

        int index = Hamming15.getErrorIndex(message, (row * COLUMN_COUNT) + 1);
        int column = getColumn(index);

        //If the candidate index is in range and the column for the candidate has an error, proceed
        if(0 <= index && index <= 195 && columns.get(column) && !path.contains(index))
        {
            message.flip(index);

            if(isRowCorrect(row, message))
            {
                path.add(index);

                //If the column is now correct, stop here and return the path as the solution
                if(isColumnCorrect(column, message))
                {
                    columns.clear(column);
                    rows.clear(row);
                    return path;
                }

                //Recursively test bit flipping in the column.  A non-empty list is the solution set.
                List<Integer> solution = correctColumn(column, message, columns, rows, depth + 1, pursue, path);

                if(!solution.isEmpty())
                {
                    rows.clear(row);
                    return solution;
                }
                else if(pursue) //Pursue additional row flips
                {
                    for(int row2 = rows.nextSetBit(row + 1); row2 >= 0 && row2 < ROW_COUNT;
                        row2 = rows.nextSetBit(row2 + 1))
                    {
                        List<Integer> solution2 = correctRow(row2, message, columns, rows, depth + 1, pursue, path);

                        if(!solution2.isEmpty() && !solution2.contains(index))
                        {
                            columns.clear(column);
                            rows.clear(row);
                            return solution2;
                        }
                    }
                }

                path.removeLast();
            }

            //Revert the change
            message.flip(index);
        }

        return Collections.emptyList();
    }

    /**
     * Identifies the error indices from the row level Hamming(15,11,3) protected words
     */
    private static List<Integer> getRowErrors(CorrectedBinaryMessage message)
    {
        List<Integer> rowErrors = new ArrayList<>();

        for(int row = 0; row < ROW_COUNT; row++)
        {
            //The first bit of the message is a pad/reserved bit and is ignored.
            int index = Hamming15.getErrorIndex(message, (row * COLUMN_COUNT) + 1);

            if(index >= 0 && index < 1000)
            {
                rowErrors.add(index);
            }
        }

        return rowErrors;
    }

    /**
     * Identifies the error indices from the column level Hamming(13,9,3) protected words
     */
    private static List<Integer> getColumnErrors(CorrectedBinaryMessage message)
    {
        List<Integer> columnErrors = new ArrayList<>();

        for(int column = 0; column < COLUMN_COUNT; column++)
        {
            int index = Hamming13.getErrorIndex(message, BPTC_196_96.COLUMN_INDEXES[column]);

            if(index >= 0 && index < 1000)
            {
                columnErrors.add(index);
            }
        }

        Collections.sort(columnErrors);
        return columnErrors;
    }

    /**
     * Values from list 1 that are also in list 2.
     */
    private static List<Integer> intersection(List<Integer> list1, List<Integer> list2)
    {
        List<Integer> intersection = new ArrayList<>();

        for(Integer i: list1)
        {
            if(list2.contains(i))
            {
                intersection.add(i);
            }
        }

        return intersection;
    }

    /**
     * Indices at the intersections of the row and column errors.
     */
    private static List<Integer> getIntersectionIndices(BinaryMessage columns, BinaryMessage rows)
    {
        List<Integer> intersections = new ArrayList<>();

        for(int row = rows.nextSetBit(0); row >= 0 && row < ROW_COUNT; row = rows.nextSetBit(row + 1))
        {
            for(int column = columns.nextSetBit(0); column >= 0 && column < COLUMN_COUNT;
                column = columns.nextSetBit(column + 1))
            {
                intersections.add(BPTC_196_96.getIndex(column, row));
            }
        }

        return intersections;
    }

    private static boolean isRowCorrect(int row, CorrectedBinaryMessage message)
    {
        return Hamming15.getSyndrome(message, (row * COLUMN_COUNT) + 1) == 0;
    }

    private static boolean isColumnCorrect(int column, CorrectedBinaryMessage message)
    {
        return Hamming13.getSyndrome(message, BPTC_196_96.COLUMN_INDEXES[column]) == 0;
    }

    private static int getColumn(int index)
    {
        return (index - 1) % COLUMN_COUNT;
    }

    private static int getRow(int index)
    {
        return (index - 1) / COLUMN_COUNT;
    }
}
