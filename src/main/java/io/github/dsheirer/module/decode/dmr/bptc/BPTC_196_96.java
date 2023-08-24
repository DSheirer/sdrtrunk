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
import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.Hamming13;
import io.github.dsheirer.edac.Hamming15;
import io.github.dsheirer.edac.IHamming;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Block Product Turbo Code (BPTC) 196/96 Support
 */
public class BPTC_196_96
{
    public static final int BPTC_LENGTH = 196;
    public static final int EXTRACTED_LENGTH = 96; //However, we set the 3x reserved bits in 96, 97, and 98 making the length 99
    public static final int MAX_ORIGINAL_INDEX = 136;
    public static final int COLUMN_COUNT = 15;
    public static final int MESSAGE_COLUMN_COUNT = 12; //Should be 11, but adjusted for the first pad bit
    public static final int CHECKSUM_COLUMN_COUNT = 4;
    public static final int MESSAGE_START_INDEX = 4;
    public static final int MAXIMUM_RECURSION_TURBO_DEPTH = 20;

    /**
     * DMR De-interleaving indices
     */
    public static int[] BPTC_DEINTERLEAVE = new int[]{0, 181, 166, 151, 136, 121, 106, 91, 76, 61, 46, 31, 16, 1, 182,
        167, 152, 137, 122, 107, 92, 77, 62, 47, 32, 17, 2, 183, 168, 153, 138, 123, 108, 93, 78, 63, 48, 33, 18, 3,
        184, 169, 154, 139, 124, 109, 94, 79, 64, 49, 34, 19, 4, 185, 170, 155, 140, 125, 110, 95, 80, 65, 50, 35, 20,
        5, 186, 171, 156, 141, 126, 111, 96, 81, 66, 51, 36, 21, 6, 187, 172, 157, 142, 127, 112, 97, 82, 67, 52, 37,
        22, 7, 188, 173, 158, 143, 128, 113, 98, 83, 68, 53, 38, 23, 8, 189, 174, 159, 144, 129, 114, 99, 84, 69, 54,
        39, 24, 9, 190, 175, 160, 145, 130, 115, 100, 85, 70, 55, 40, 25, 10, 191, 176, 161, 146, 131, 116, 101, 86, 71,
        56, 41, 26, 11, 192, 177, 162, 147, 132, 117, 102, 87, 72, 57, 42, 27, 12, 193, 178, 163, 148, 133, 118, 103,
        88, 73, 58, 43, 28, 13, 194, 179, 164, 149, 134, 119, 104, 89, 74, 59, 44, 29, 14, 195, 180, 165, 150, 135, 120,
        105, 90, 75, 60, 45, 30, 15};

    /**
     * DMR Column Indices
     */
    public static int[][] COLUMN_INDEXES = new int[][]{
        {1, 16, 31, 46, 61, 76, 91, 106, 121, 136, 151, 166, 181},
        {2, 17, 32, 47, 62, 77, 92, 107, 122, 137, 152, 167, 182},
        {3, 18, 33, 48, 63, 78, 93, 108, 123, 138, 153, 168, 183},
        {4, 19, 34, 49, 64, 79, 94, 109, 124, 139, 154, 169, 184},
        {5, 20, 35, 50, 65, 80, 95, 110, 125, 140, 155, 170, 185},
        {6, 21, 36, 51, 66, 81, 96, 111, 126, 141, 156, 171, 186},
        {7, 22, 37, 52, 67, 82, 97, 112, 127, 142, 157, 172, 187},
        {8, 23, 38, 53, 68, 83, 98, 113, 128, 143, 158, 173, 188},
        {9, 24, 39, 54, 69, 84, 99, 114, 129, 144, 159, 174, 189},
        {10, 25, 40, 55, 70, 85, 100, 115, 130, 145, 160, 175, 190},
        {11, 26, 41, 56, 71, 86, 101, 116, 131, 146, 161, 176, 191},
        {12, 27, 42, 57, 72, 87, 102, 117, 132, 147, 162, 177, 192},
        {13, 28, 43, 58, 73, 88, 103, 118, 133, 148, 163, 178, 193},
        {14, 29, 44, 59, 74, 89, 104, 119, 134, 149, 164, 179, 194},
        {15, 30, 45, 60, 75, 90, 105, 120, 135, 150, 165, 180, 195}};

    /**
     * Performs de-interleave, error detect and correct and extracts a 96-bit message from the DMR BPTC(196,96)
     * protected message
     * @param original uncorrected BPTC(196,96) message
     * @return error corrected and extracted 96-bit message
     */
    public static CorrectedBinaryMessage extract(CorrectedBinaryMessage original)
    {
        //De-Interleave
        CorrectedBinaryMessage message = deinterleave(original);

        //Perform error detection and correction
        boolean pursueShadows = true;
        correct(message, pursueShadows);

        //Extract the 96-bit message from the corrected 196 bits
        CorrectedBinaryMessage extracted = new CorrectedBinaryMessage(EXTRACTED_LENGTH);

        //Transfer the corrected bit count
        extracted.setCorrectedBitCount(message.getCorrectedBitCount());

        int index = MESSAGE_START_INDEX;

        while(index < MAX_ORIGINAL_INDEX)
        {
            if((index % COLUMN_COUNT) < MESSAGE_COLUMN_COUNT)
            {
                try
                {
                    extracted.add(message.get(index));
                }
                catch(BitSetFullException bsfe)
                {
                    //Should never get to here
                    bsfe.printStackTrace();
                    return extracted;
                }

                index++;
            }
            else
            {
                index += CHECKSUM_COLUMN_COUNT;
            }
        }

        //Transfer bits R2, R1, and R0 to the end - RAS bits
        extracted.set(96, message.get(0));
        extracted.set(97, message.get(1));
        extracted.set(98, message.get(2));

        return extracted;
    }

    /**
     * De-interleaves the transmitted bits of a DMR BPTC(196,96) protected message
     * @param message that contains interleaved (transmitted) message
     * @return de-interleaved message
     */
    private static CorrectedBinaryMessage deinterleave(CorrectedBinaryMessage message)
    {
        CorrectedBinaryMessage deinterleaved = new CorrectedBinaryMessage(BPTC_LENGTH);
        for(int x = 0; x < BPTC_DEINTERLEAVE.length; x++)
        {
            deinterleaved.set(x, message.get(BPTC_DEINTERLEAVE[x]));
        }

        return deinterleaved;
    }

    /**
     * Identifies the error indices from the row level Hamming(15,11,3) protected words
     * @param message containing protected words in rows
     * @return list of zero or more error indices
     */
    private static List<Integer> getRowErrors(CorrectedBinaryMessage message)
    {
        List<Integer> rowErrors = new ArrayList<>();

        int offset = 0;
        int index = -1;

        for(int row = 0; row < 13; row++)
        {
            //The first bit of the message is a pad/reserved bit and is ignored.
            offset = (row * 15) + 1;
            index = Hamming15.getErrorIndex(message, offset);

            if(index >= 0 && index < 1000)
            {
                rowErrors.add(index);
            }
        }

        //Note: row errors should already be in order and don't need sorted
        return rowErrors;
    }

    /**
     * Identifies the error indices from the column level Hamming(13,9,3) protected words
     * @param message containing protected words in columns
     * @return list of zero or more error indices
     */
    private static List<Integer> getColumnErrors(CorrectedBinaryMessage message)
    {
        List<Integer> columnErrors = new ArrayList<>();

        int index = -1;

        for(int column = 0; column < 15; column++)
        {
            index = Hamming13.getErrorIndex(message, COLUMN_INDEXES[column]);

            if(index >= 0 && index < 1000)
            {
                columnErrors.add(index);
            }
        }

        Collections.sort(columnErrors);
        return columnErrors;
    }

    /**
     * Creates a list of the values that intersect between list 1 and list 2.
     * @param list1 to compare
     * @param list2 to compare
     * @return intersection
     */
    private static List<Integer> intersection(List<Integer> list1, List<Integer> list2)
    {
        List<Integer> intersection = new ArrayList<>();

        for(Integer i : list1)
        {
            if(list2.contains(i))
            {
                intersection.add(i);
            }
        }

        return intersection;
    }

    private static void logDiagnostic(CorrectedBinaryMessage message)
    {
        StringBuilder sb = new StringBuilder();
        for(int row = 0; row < 13; row++)
        {
            int offset = (row * 15) + 1;
            sb.append("Row ");
            if(row < 10)
            {
                sb.append(" ").append(row);
            }
            else
            {
                sb.append(row);
            }
            sb.append(": ");
            sb.append(message.getSubMessage(offset, offset + 16));
            int index = getRowErrorIndex(row, message);
            sb.append(" Index:").append(index == IHamming.NO_ERRORS ? "-": index);
            sb.append("\n");
        }

        sb.append("Column Error Indices: ");
        for(int column = 0; column < 15; column++)
        {
            int index = getColumnErrorIndex(column, message);
            String label = index == IHamming.NO_ERRORS ? "-" : (index == 1000 ? "(bad)" : String.valueOf(index));
            sb.append(column).append(":").append(label).append(" ");
        }

        System.out.println("Diagnostic:\n" + sb + "\n");
    }

    private static void logErrorMap(CorrectedBinaryMessage message)
    {
        StringBuilder sb = new StringBuilder();
        for(int row = 0; row < 13; row++)
        {
            int offset = (row * 15) + 1;
            sb.append("Row ");
            if(row < 10)
            {
                sb.append(" ").append(row);
            }
            else
            {
                sb.append(row);
            }
            sb.append(": ");
            sb.append(message.getSubMessage(offset, offset + 15));

            sb.append(" ").append(offset).append(":").append(offset + 14).append(" =");

            for(int index = offset; index < offset + 15; index++)
            {
                if(message.get(index))
                {
                    sb.append(" " + index);
                }
            }
            sb.append("\n");
        }

        System.out.println("Error Map:\n" + sb + "\n");
    }

    /**
     * Calculates the indices that make up the intersections of the row and column errors.
     * @param rows bitmap indicating rows with errors
     * @param columns bitmap indicating columns with errors
     * @return set of intersection indices.
     */
    public static List<Integer> getIntersectionIndices(BinaryMessage columns, BinaryMessage rows)
    {
        List<Integer> intersections = new ArrayList<>();

        for(int row = rows.nextSetBit(0); row >= 0 && row < 13; row = rows.nextSetBit(row + 1))
        {
            for(int column = columns.nextSetBit(0); column >= 0 && column < 15; column = columns.nextSetBit(column + 1))
            {
                intersections.add(getIndex(column, row));
            }
        }

        return intersections;
    }

    /**
     * Performs turbo decode of the message by testing multiple paths across rows and columns to identify paths
     * that resolve errors validated by the Hamming checks.  Can also detect and often correct shadowing where the bit
     * errors occur in a 2x2, 2x3, 3x2 or 3x3 column/row grid and can't normally be corrected by the Hamming code.
     * @param message to correct.
     * @param pursueShadows to test intersection flipping for double/triple shadows.
     * @return true if the message was corrected, false otherwise.
     */
    public static boolean correct(CorrectedBinaryMessage message, boolean pursueShadows)
    {
        List<Integer> columnErrors = getColumnErrors(message);
        List<Integer> rowErrors = getRowErrors(message);

        if(columnErrors.isEmpty() && rowErrors.isEmpty())
        {
            return true;
        }

        //Fix the easy bits first.
        List<Integer> flippedBits = new ArrayList<>();
        List<Integer> intersection = intersection(columnErrors, rowErrors);

        if(!intersection.isEmpty())
        {
            for(Integer i : intersection)
            {
                message.flip(i);
                flippedBits.add(i);
            }

            message.incrementCorrectedBitCount(intersection.size());
        }

        //Set of flags for columns with errors
        BinaryMessage columns = new BinaryMessage(15);
        for(int column = 0; column < 15; column++)
        {
            columns.set(column, !isColumnCorrect(column, message));
        }

        //Set of flags for rows with errors
        BinaryMessage rows = new BinaryMessage(13);
        for(int row = 0; row < 13; row++)
        {
            rows.set(row, !isRowCorrect(row, message));
        }

        if(columns.isEmpty() && rows.isEmpty())
        {
            return true; //No (more) errors
        }

        //1: fix up to 2-bit errors per column or row without pursuing multi-column or multi-row paths
        boolean pursue = false;

        for(int column = columns.nextSetBit(0); column >= 0 && column < 15; column = columns.nextSetBit(column + 1))
        {
            List<Integer> solution = correctColumn(column, message, columns, rows, 0, pursue, new ArrayList<>());
            flippedBits.addAll(solution);
        }

        if(!columns.isEmpty() || !rows.isEmpty())
        {
            for(int row = rows.nextSetBit(0); row >= 0 && row < 13; row = rows.nextSetBit(row + 1))
            {
                List<Integer> solution = correctRow(row, message, columns, rows, 0, pursue, new ArrayList<>());
                flippedBits.addAll(solution);
            }
        }

        if(!columns.isEmpty() || !rows.isEmpty())
        {
            pursue = true;

            for(int column = columns.nextSetBit(0); column >= 0 && column < 15; column = columns.nextSetBit(column + 1))
            {
                List<Integer> solution = correctColumn(column, message, columns, rows, 0, pursue, new ArrayList<>());
                flippedBits.addAll(solution);
            }

            if(!columns.isEmpty() || !rows.isEmpty())
            {
                for(int row = rows.nextSetBit(0); row >= 0 && row < 13; row = rows.nextSetBit(row + 1))
                {
                    List<Integer> solution = correctRow(row, message, columns, rows, 0, pursue, new ArrayList<>());
                    flippedBits.addAll(solution);
                }
            }
        }

        //Detect double/triple row/column bit errors that are shadowing each other and preventing the
        // Hamming codes from giving accurate bit error locations.  Flip all of their intersections and
        // then attempt to correct the message again.  We use a flag 'pursueShadows' to ensure that we
        // only chase the shadow once, not recursively, since this parent method can be called recursively.
        if(pursueShadows && ((columns.cardinality() == 2 || columns.cardinality() == 3) &&
                (rows.cardinality() == 2 || rows.cardinality() == 3)))
        {
            List<Integer> intersectionIndices = getIntersectionIndices(columns, rows);
            for(Integer intersectionIndex: intersectionIndices)
            {
                message.flip(intersectionIndex);
            }

            //Set pursue shadows to false so we don't recursively chase shadows
            pursueShadows = false;

            if(correct(message, pursueShadows))
            {
                columns.clear();
                rows.clear();
                int correctedBits = intersectionIndices.size() - message.getCorrectedBitCount();
                message.incrementCorrectedBitCount(correctedBits);
            }
            else
            {
                for(Integer intersectionIndex: intersectionIndices)
                {
                    message.flip(intersectionIndex);
                }
            }
        }

        if(columns.isEmpty() && rows.isEmpty())
        {
            Collections.sort(flippedBits);
            message.incrementCorrectedBitCount(flippedBits.size());
        }
        else
        {
            message.setCorrectedBitCount(BPTCBase.ERRORS_NOT_CORRECTED);
            return false;
        }

        return true;
    }

    /**
     * Recursive method ...
     * @param column
     * @param message
     * @param columns
     * @param rows
     * @return
     */
    public static List<Integer> correctColumn(int column, CorrectedBinaryMessage message, BinaryMessage columns,
                                              BinaryMessage rows, int depth, boolean pursue, List<Integer> path)
    {
        //Don't let the recursive call exceed threshold
        if(depth > MAXIMUM_RECURSION_TURBO_DEPTH)
        {
            return Collections.emptyList();
        }

        int index = getColumnErrorIndex(column, message);
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

                //If our candidate index is part of the discovered solution, fail, otherwise return it as part of
                // the solution.
                if(solution.size() > 0)
                {
                    columns.clear(column);
                    return solution;
                }
                else if(pursue) //Pursue additional column flips
                {
                    for(int column2 = columns.nextSetBit(column + 1); column2 >= 0 && column2 < 15; column2 = columns.nextSetBit(column2 + 1))
                    {
                        List<Integer> solution2 = correctColumn(column2, message, columns, rows, depth + 1, pursue, path);

                        if(solution2.size() > 0)
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

    public static List<Integer> correctRow(int row, CorrectedBinaryMessage message, BinaryMessage columns,
                                           BinaryMessage rows, int depth, boolean pursue, List<Integer> path)
    {
        //Don't let the recursive call exceed threshold
        if(depth > MAXIMUM_RECURSION_TURBO_DEPTH)
        {
            return Collections.emptyList();
        }

        int index = getRowErrorIndex(row, message);
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

                //If our candidate index is part of the discovered solution, fail, otherwise return it as part of
                // the solution.
                if(solution.size() > 0)
                {
                    rows.clear(row);
                    return solution;
                }
                else if(pursue) //Pursue additional row flips
                {
                    for(int row2 = rows.nextSetBit(row + 1); row2 >= 0 && row2 < 13; row2 = rows.nextSetBit(row2 + 1))
                    {
                        List<Integer> solution2 = correctRow(row2, message, columns, rows, depth + 1, pursue, path);

                        if(solution2.size() > 0 && !solution2.contains(index))
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

    public static int getIndex(int column, int row)
    {
        return row * 15 + column + 1;
    }

    /**
     * Checks the row for bit errors.
     * @param row to check
     * @param message containing bits to check
     * @return true if the row passes the Hamming check or false if not.
     */
    private static boolean isRowCorrect(int row, CorrectedBinaryMessage message)
    {
        return Hamming15.getSyndrome(message, (row * 15) + 1) == 0;
    }

    /**
     * Calculates the index for the bit error in the message
     * @param row to check
     * @param message with bits
     * @return error index in the range 0-195, or -1 if the message row has no bit errors.
     */
    private static int getRowErrorIndex(int row, CorrectedBinaryMessage message)
    {
        return Hamming15.getErrorIndex(message, (row * 15) + 1);
    }

    /**
     * Checks the column for bit errors.
     * @param column to check
     * @param message containing bits to check
     * @return true if the column passes the Hamming check or false if not.
     */
    private static boolean isColumnCorrect(int column, CorrectedBinaryMessage message)
    {
        return Hamming13.getSyndrome(message, COLUMN_INDEXES[column]) == 0;
    }

    /**
     * Calculates the index for the bit error in the message.
     * @param column to check
     * @param message with bits.
     * @return error index in the range 0-195, or -1 if message has no bit errors, or 1000 if message has more than 1 bit error.
     */
    private static int getColumnErrorIndex(int column, CorrectedBinaryMessage message)
    {
        return Hamming13.getErrorIndex(message, COLUMN_INDEXES[column]);
    }

    public static int getColumn(int index)
    {
        return (index - 1) % 15;
    }

    public static int getRow(int index)
    {
        return (index - 1) / 15;
    }

    public static void main(String[] args)
    {
        String raw = "0000101111101000000100001010100001010000001000000011100000000000100000000000001000000000001000000000000010000000001111001010000100001000000001100000100001000111110101011111011111101001111011100010";
        String ref = "0000101111100000000100001100100001010000001000000000000000000000000000000000000000000000000000000000000000000000001111001010000100001000000001110000100001011111110101011011011111101011111011100000";
        CorrectedBinaryMessage rawMessage = new CorrectedBinaryMessage(BinaryMessage.load(raw));
        CorrectedBinaryMessage refMessage = new CorrectedBinaryMessage(BinaryMessage.load(ref));

//        correct2(refMessage);

        boolean pursueShadows = true;
        correct(rawMessage, pursueShadows);
        System.out.println("RAW: " + raw);
        System.out.println("COR: " + rawMessage);
        System.out.println("REF: " + ref);

        rawMessage.xor(refMessage);
        System.out.println("RES: " + rawMessage);

        if(rawMessage.cardinality() > 0)
        {
            System.out.println("Residual Error Map:");
            logErrorMap(rawMessage);
        }

        rawMessage = new CorrectedBinaryMessage(BinaryMessage.load(raw));
        rawMessage.xor(refMessage);
        System.out.println("\n\nXOR: " + rawMessage);
        logErrorMap(rawMessage);
    }
}
