package io.github.dsheirer.edac;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Block Product Turbo Code (BPTC) 196/96 Support
 */
public class BPTC_196_96
{
    public static final int BPTC_LENGTH = 196;
    public static final int EXTRACTED_LENGTH = 96;
    public static final int MAX_ORIGINAL_INDEX = 136;
    public static final int COLUMN_COUNT = 15;
    public static final int MESSAGE_COLUMN_COUNT = 12; //Should be 11, but adjusted for the first pad bit
    public static final int CHECKSUM_COLUMN_COUNT = 4;
    public static final int MESSAGE_START_INDEX = 4;

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
        correct(message);

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

            if(index >= 0)
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

            if(index >= 0)
            {
                columnErrors.add(index);
            }
        }

        Collections.sort(columnErrors);
        return columnErrors;
    }

    /**
     * Performs error detection and correction on the DMR BPTC(196,96) protected message
     *
     * @param message to process and extract
     */
    private static void correct(CorrectedBinaryMessage message)
    {
        List<Integer> rowErrors = getRowErrors(message);

        if(!rowErrors.isEmpty())
        {
            List<Integer> columnErrors = getColumnErrors(message);

            if(matches(rowErrors, columnErrors))
            {
                for(Integer index: rowErrors)
                {
                    message.flip(index);
                }

                message.setCorrectedBitCount(rowErrors.size());
            }
            else
            {
                //Step 1: correct row errors and recalculate column errors (should be zero)
                for(Integer index: rowErrors)
                {
                    message.flip(index);
                }

                List<Integer> columnErrorsPart2 = getColumnErrors(message);

                if(columnErrorsPart2.isEmpty())
                {
                    //Success
                    message.setCorrectedBitCount(rowErrors.size());
                }
                else
                {
                    //Reverse the row error correction
                    for(Integer index: rowErrors)
                    {
                        message.flip(index);
                    }

                    //Step 2: correct column errors and recalculate row errors (should be zero)
                    for(Integer index: columnErrors)
                    {
                        message.flip(index);
                    }

                    List<Integer> rowErrorsPart2 = getRowErrors(message);

                    if(rowErrorsPart2.isEmpty())
                    {
                        //Success
                        message.setCorrectedBitCount(columnErrors.size());
                    }
                    else
                    {
                        //Reverse the column error correction
                        for(Integer index: columnErrors)
                        {
                            message.flip(index);
                        }

                        //Set the error count greater than the maximum correctable column errors (16)
                        message.setCorrectedBitCount(16);
                    }
                }
            }
        }
    }

    /**
     * Indicates if the errors detected by the row protected words match the errors detected by the column protected words
     * @param rowErrors bit positions
     * @param columnErrors bit positions
     * @return true if they have the same quantity of bit position errors and the bit positions match
     */
    private static boolean matches(List<Integer> rowErrors, List<Integer> columnErrors)
    {
        if(rowErrors.size() == columnErrors.size())
        {
            for(int x = 0; x < rowErrors.size(); x++)
            {
                if(!rowErrors.get(x).equals(columnErrors.get(0)))
                {
                    return false;
                }
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    public static void main(String[] args)
    {
        String raw = "0000000001100010100101100011100100110100110101000100000011101001110001001000100001001010010100001000011111011000000011110000111000111111100011000011100001100000000101001010000100010111010100111001";
        System.out.println("RAW:" + raw);

        CorrectedBinaryMessage message = new CorrectedBinaryMessage(BinaryMessage.load(raw));
        System.out.println("LOD:" + message.toString());
        CorrectedBinaryMessage extracted = extract(message);
        System.out.println("EXT:" + extracted.toString());
    }
}
