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
import io.github.dsheirer.edac.ProductCodeDecoder;
import io.github.dsheirer.edac.soft.SoftBlockCode;

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
    private static final int ROW_COUNT = 13;
    private static final int ROW_LENGTH = 15;
    private static final int COLUMN_LENGTH = 13;

    //Soft decision (Chase-Pyndiah) iterative decoding settings.  Each half iteration decodes either all rows or all
    //columns, starting with the rows.  Alpha scales the extrinsic information fed to the next half iteration and beta
    //is the reliability assigned to bits without a competing candidate codeword.
    private static final int CHASE_LEAST_RELIABLE_BITS = 4;
    private static final float[] SOFT_ALPHA = new float[]{0.0f, 0.2f, 0.3f, 0.5f, 0.7f, 0.9f, 1.0f, 1.0f};
    private static final float[] SOFT_BETA = new float[]{0.2f, 0.4f, 0.6f, 0.8f, 1.0f, 1.0f, 1.0f, 1.0f};
    private static final int[] COLUMN_WORD_INDEXES = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    private static final SoftBlockCode ROW_CODE = SoftBlockCode.fromCodewordTest(ROW_LENGTH,
            word -> Hamming15.getSyndrome(toMessage(word, ROW_LENGTH), 0) == 0);
    private static final SoftBlockCode COLUMN_CODE = SoftBlockCode.fromCodewordTest(COLUMN_LENGTH,
            word -> Hamming13.getSyndrome(toMessage(word, COLUMN_LENGTH), COLUMN_WORD_INDEXES) == 0);

    //Maximum number of bit errors corrected by the hard decision minimum distance decoder.  The product code minimum
    //distance of 9 guarantees correction of 4 bit errors.  Miscorrections are rejected by the message CRC check, which
    //then retries with soft decision decoding.  Error injection testing: corrects 99.7% of 8-bit error patterns and the
    //largest search for random error patterns took 3 ms.  Messages with more bit errors fall back to turbo decoding.
    private static final int HARD_MAXIMUM_ERRORS = 12;
    private static final ProductCodeDecoder HARD_DECODER = new ProductCodeDecoder(
            ProductCodeDecoder.getParityChecks(ROW_LENGTH, message -> Hamming15.getSyndrome(message, 0)),
            ProductCodeDecoder.getParityChecks(COLUMN_LENGTH,
                    message -> Hamming13.getSyndrome(message, COLUMN_WORD_INDEXES)), HARD_MAXIMUM_ERRORS);

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
     * DMR Row Indices
     */
    public static final int[][] ROW_INDEXES = new int[ROW_COUNT][ROW_LENGTH];

    static
    {
        for(int row = 0; row < ROW_COUNT; row++)
        {
            for(int x = 0; x < ROW_LENGTH; x++)
            {
                ROW_INDEXES[row][x] = getIndex(x, row);
            }
        }
    }

    /**
     * Creates a binary message from the word where bit i of the word is message bit i.
     */
    private static BinaryMessage toMessage(int word, int length)
    {
        BinaryMessage message = new BinaryMessage(length);

        for(int i = 0; i < length; i++)
        {
            if((word & (1 << i)) != 0)
            {
                message.set(i);
            }
        }

        return message;
    }

    /**
     * Performs de-interleave, error detect and correct and extracts a 96-bit message from the DMR BPTC(196,96)
     * protected message
     * @param original uncorrected BPTC(196,96) message
     * @return error corrected and extracted 96-bit message
     */
    public static CorrectedBinaryMessage extract(CorrectedBinaryMessage original)
    {
        return extract(original, null);
    }

    /**
     * Performs de-interleave, error detect and correct and extracts a 96-bit message from the DMR BPTC(196,96)
     * protected message.  When the hard decision decoder can't correct the message and soft bits are provided,
     * applies iterative soft decision decoding.
     * @param original uncorrected BPTC(196,96) message
     * @param softBits for the 196 (interleaved) bits of the original message, or null.
     * @return error corrected and extracted 96-bit message
     */
    public static CorrectedBinaryMessage extract(CorrectedBinaryMessage original, float[] softBits)
    {
        //De-Interleave
        CorrectedBinaryMessage message = deinterleave(original);

        //Perform error detection and correction
        correct(message);

        if(softBits != null && softBits.length == BPTC_LENGTH && message.getCorrectedBitCount() < 0)
        {
            CorrectedBinaryMessage softDecoded = decodeSoft(deinterleave(softBits));

            if(softDecoded != null)
            {
                message = softDecoded;
            }
        }

        return extractPayload(message);
    }

    /**
     * Applies soft decision decoding to the soft bits and extracts the 96-bit message.  Can be used when a hard
     * decision result fails a subsequent message integrity (ie CRC) check.
     * @param softBits for the 196 (interleaved) bits of the message where a positive value favors a 1 bit.
     * @return error corrected and extracted 96-bit message, or null if soft decision decoding doesn't converge.
     */
    public static CorrectedBinaryMessage extractSoft(float[] softBits)
    {
        if(softBits == null || softBits.length != BPTC_LENGTH)
        {
            return null;
        }

        CorrectedBinaryMessage decoded = decodeSoft(deinterleave(softBits));
        return decoded != null ? extractPayload(decoded) : null;
    }

    /**
     * Extracts the 96-bit message and the 3 reserved bits from the corrected (de-interleaved) 196-bit message.
     * @param message corrected and de-interleaved.
     * @return extracted message with the corrected bit count transferred.
     */
    private static CorrectedBinaryMessage extractPayload(CorrectedBinaryMessage message)
    {
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
     * De-interleaves the transmitted soft bits of a DMR BPTC(196,96) protected message
     * @param softBits that contains interleaved (transmitted) soft bits
     * @return de-interleaved soft bits
     */
    private static float[] deinterleave(float[] softBits)
    {
        float[] deinterleaved = new float[BPTC_LENGTH];

        for(int x = 0; x < BPTC_DEINTERLEAVE.length; x++)
        {
            deinterleaved[x] = softBits[BPTC_DEINTERLEAVE[x]];
        }

        return deinterleaved;
    }

    /**
     * Iterative soft decision (Chase-Pyndiah turbo product) decoding of the de-interleaved BPTC(196,96) soft bits.
     * Alternately decodes the Hamming(15,11) rows and Hamming(13,9) columns, exchanging extrinsic soft information,
     * until the decisions form a valid product codeword.
     * @param channel de-interleaved soft bits where a positive value favors a 1 bit.
     * @return decoded message with the corrected bit count set, or null if decoding doesn't converge.
     */
    public static CorrectedBinaryMessage decodeSoft(float[] channel)
    {
        //Normalize the channel soft bits to a mean magnitude of 1 for the alpha and beta settings
        float sum = 0.0f;

        for(int i = 1; i < BPTC_LENGTH; i++)
        {
            sum += Math.abs(channel[i]);
        }

        if(sum <= 0.0f)
        {
            return null;
        }

        float scale = (BPTC_LENGTH - 1) / sum;
        float[] received = new float[BPTC_LENGTH];

        for(int i = 0; i < BPTC_LENGTH; i++)
        {
            received[i] = channel[i] * scale;
        }

        float[] extrinsic = new float[BPTC_LENGTH];
        float[] input = new float[BPTC_LENGTH];
        float[] output = new float[BPTC_LENGTH];
        int[] rowWords = new int[ROW_COUNT];
        int[] columnWords = new int[COLUMN_COUNT];

        for(int halfIteration = 0; halfIteration < SOFT_ALPHA.length; halfIteration++)
        {
            float alpha = SOFT_ALPHA[halfIteration];
            float beta = SOFT_BETA[halfIteration];

            for(int i = 0; i < BPTC_LENGTH; i++)
            {
                input[i] = received[i] + alpha * extrinsic[i];
            }

            boolean decodeRows = halfIteration % 2 == 0;
            boolean allDecoded = true;

            if(decodeRows)
            {
                for(int row = 0; row < ROW_COUNT; row++)
                {
                    rowWords[row] = ROW_CODE.decodeChase(input, ROW_INDEXES[row], output, CHASE_LEAST_RELIABLE_BITS, beta);
                    allDecoded &= rowWords[row] >= 0;
                }
            }
            else
            {
                for(int column = 0; column < COLUMN_COUNT; column++)
                {
                    columnWords[column] = COLUMN_CODE.decodeChase(input, COLUMN_INDEXES[column], output,
                            CHASE_LEAST_RELIABLE_BITS, beta);
                    allDecoded &= columnWords[column] >= 0;
                }
            }

            //Check for a valid product codeword from the decisions of this half iteration
            if(allDecoded)
            {
                CorrectedBinaryMessage decoded = new CorrectedBinaryMessage(BPTC_LENGTH);

                if(received[0] > 0)
                {
                    decoded.set(0);
                }

                int[][] decodedIndexes = decodeRows ? ROW_INDEXES : COLUMN_INDEXES;
                int[] decodedWords = decodeRows ? rowWords : columnWords;

                for(int w = 0; w < decodedWords.length; w++)
                {
                    for(int i = 0; i < decodedIndexes[w].length; i++)
                    {
                        if((decodedWords[w] & (1 << i)) != 0)
                        {
                            decoded.set(decodedIndexes[w][i]);
                        }
                    }
                }

                if(isValidProductCodeword(decoded))
                {
                    int corrected = 0;

                    for(int i = 0; i < BPTC_LENGTH; i++)
                    {
                        if(decoded.get(i) != (received[i] > 0))
                        {
                            corrected++;
                        }
                    }

                    decoded.setCorrectedBitCount(corrected);
                    return decoded;
                }
            }

            //Normalize the extrinsic information to a mean magnitude of 1 for the next half iteration
            float extrinsicSum = 0.0f;

            for(int i = 1; i < BPTC_LENGTH; i++)
            {
                extrinsicSum += Math.abs(output[i]);
            }

            float extrinsicScale = extrinsicSum > 0.0f ? (BPTC_LENGTH - 1) / extrinsicSum : 0.0f;

            for(int i = 0; i < BPTC_LENGTH; i++)
            {
                extrinsic[i] = output[i] * extrinsicScale;
            }
        }

        return null;
    }

    /**
     * Indicates if all rows and columns of the de-interleaved message pass the Hamming checks.
     */
    private static boolean isValidProductCodeword(CorrectedBinaryMessage message)
    {
        for(int row = 0; row < ROW_COUNT; row++)
        {
            if(!isRowCorrect(row, message))
            {
                return false;
            }
        }

        for(int column = 0; column < COLUMN_COUNT; column++)
        {
            if(!isColumnCorrect(column, message))
            {
                return false;
            }
        }

        return true;
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
     * Performs hard decision error detection and correction of the de-interleaved message, using minimum distance
     * decoding, followed by turbo decoding when the minimum distance decoder can't correct the message.  When
     * corrected, the corrected bit count is set to the number of corrected bits.  Otherwise, the message is unchanged
     * and the corrected bit count is set to ERRORS_NOT_CORRECTED.  Note: the leading pad bit is not checked.
     *
     * @param message to correct.
     * @return true if the message was corrected, false otherwise.
     */
    public static boolean correct(CorrectedBinaryMessage message)
    {
        int[] rows = getRows(message);
        int corrected = HARD_DECODER.decode(rows);

        if(corrected == ProductCodeDecoder.UNCORRECTABLE)
        {
            //Fallback: real-world messages can have more bit errors than the minimum distance decoder maximum, spread
            //across the rows and columns, which the turbo decoder can often correct.  Recording testing: recovered 29
            //of 30 CRC verified messages lost by the minimum distance decoder alone.
            CorrectedBinaryMessage turbo = new CorrectedBinaryMessage(message);

            if(new BPTC_196_96TurboDecoder().correct(turbo))
            {
                rows = getRows(turbo);

                if(HARD_DECODER.isValid(rows))
                {
                    int[] received = getRows(message);
                    corrected = 0;

                    for(int row = 0; row < ROW_COUNT; row++)
                    {
                        corrected += Integer.bitCount(rows[row] ^ received[row]);
                    }
                }
            }
        }

        if(corrected == ProductCodeDecoder.UNCORRECTABLE)
        {
            message.setCorrectedBitCount(BPTCBase.ERRORS_NOT_CORRECTED);
            return false;
        }

        if(corrected > 0)
        {
            for(int row = 0; row < ROW_COUNT; row++)
            {
                for(int column = 0; column < ROW_LENGTH; column++)
                {
                    message.set(getIndex(column, row), (rows[row] & (1 << column)) != 0);
                }
            }
        }

        message.setCorrectedBitCount(corrected);
        return true;
    }

    /**
     * Row words of the de-interleaved message, where bit i of each row is the bit in column i.
     */
    private static int[] getRows(CorrectedBinaryMessage message)
    {
        int[] rows = new int[ROW_COUNT];

        for(int row = 0; row < ROW_COUNT; row++)
        {
            for(int column = 0; column < ROW_LENGTH; column++)
            {
                if(message.get(getIndex(column, row)))
                {
                    rows[row] |= 1 << column;
                }
            }
        }

        return rows;
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

    public static void main(String[] args)
    {
        String raw = "0000101111101000000100001010100001010000001000000011100000000000100000000000001000000000001000000000000010000000001111001010000100001000000001100000100001000111110101011111011111101001111011100010";
        String ref = "0000101111100000000100001100100001010000001000000000000000000000000000000000000000000000000000000000000000000000001111001010000100001000000001110000100001011111110101011011011111101011111011100000";
        CorrectedBinaryMessage rawMessage = new CorrectedBinaryMessage(BinaryMessage.load(raw));
        CorrectedBinaryMessage refMessage = new CorrectedBinaryMessage(BinaryMessage.load(ref));

//        correct2(refMessage);

        correct(rawMessage);
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
