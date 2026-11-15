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
import io.github.dsheirer.edac.Hamming16;
import io.github.dsheirer.edac.ProductCodeDecoder;

/**
 * Block Product Turbo Code 16/2 for decoding a DMR Voice Frame F payload from the 32-bit EMB field.  Each of the two
 * rows is the same Hamming(16,11,4) codeword, where the second row is inverted (odd column parity) for the reverse
 * channel, forming a code with a minimum distance of 8.
 * <p>
 * Uses hard decision minimum distance decoding across both rows, which corrects up to 3 bit errors.
 * <p>
 * See ETSI TS 102 361-1 B.2.2.1 and B.2.2.2
 */
public class BPTC_16_2
{
    private static final int[] DEINTERLEAVE = new int[]{0, 24, 1, 25, 2, 26, 3, 27, 4, 28, 5, 29, 6, 30, 7, 31, 8, 16,
            9, 17, 10, 18, 11, 19, 12, 20, 13, 21, 14, 22, 15, 23};
    private static final int ROW_LENGTH = 16;
    private static final int ROW_MASK = 0xFFFF;
    //Maximum number of bit errors to correct.  This is the guaranteed correction capability for the minimum distance of
    //8, so the nearest codeword is always unique.  Error injection testing: corrects all 1-3 bit error patterns and
    //miscorrects 3.9% of 5-bit error patterns.  At a 3% bit error rate, corrects 98.5% of messages versus 93% with a
    //maximum of 2.
    private static final int MAXIMUM_ERRORS = 3;
    private static final int[] CODEWORDS = getCodewords();

    /**
     * Identifies the Hamming(16,11,4) codewords where bit i of each codeword is message bit i.
     */
    private static int[] getCodewords()
    {
        int[] checks = ProductCodeDecoder.getParityChecks(ROW_LENGTH, message -> Hamming16.getSyndrome(message, 0));
        int[] codewords = new int[1 << 11];
        int count = 0;

        for(int word = 0; word <= ROW_MASK; word++)
        {
            if(ProductCodeDecoder.getSyndrome(word, checks) == 0)
            {
                codewords[count++] = word;
            }
        }

        return codewords;
    }

    /**
     * Unscramble and perform FEC checks per paragraph B.2.2.1 for Non-Reverse Channel Single Burst
     *
     * @param message with 32 interleaved bits.
     * @return descrambled and error corrected message or null if there are too many errors.
     */
    public static CorrectedBinaryMessage decodeShortBurst(CorrectedBinaryMessage message)
    {
        return decode(deinterleave(message), false);
    }

    /**
     * Unscramble and perform FEC checks per paragraph B.2.2.2 for Reverse Channel Single Burst
     *
     * @param message with 32 interleaved bits.
     * @return descrambled and error corrected message or null if there are too many errors.
     */
    public static CorrectedBinaryMessage decodeReverseChannel(CorrectedBinaryMessage message)
    {
        return decode(deinterleave(message), true);
    }

    /**
     * Decodes the deinterleaved message to the nearest codeword.
     * @param deinterleaved message
     * @param oddParity true when the second row is the inverse of the first row (reverse channel).
     * @return corrected message with the corrected bit count set, or null if there are too many errors.
     */
    private static CorrectedBinaryMessage decode(CorrectedBinaryMessage deinterleaved, boolean oddParity)
    {
        int row1 = 0;
        int row2 = 0;

        for(int x = 0; x < ROW_LENGTH; x++)
        {
            if(deinterleaved.get(x))
            {
                row1 |= 1 << x;
            }

            if(deinterleaved.get(x + ROW_LENGTH))
            {
                row2 |= 1 << x;
            }
        }

        if(oddParity)
        {
            row2 ^= ROW_MASK;
        }

        int bestDistance = Integer.MAX_VALUE;
        int bestCodeword = 0;

        for(int codeword: CODEWORDS)
        {
            int distance = Integer.bitCount(codeword ^ row1) + Integer.bitCount(codeword ^ row2);

            if(distance < bestDistance)
            {
                bestDistance = distance;
                bestCodeword = codeword;
            }
        }

        if(bestDistance > MAXIMUM_ERRORS)
        {
            return null;
        }

        int second = oddParity ? bestCodeword ^ ROW_MASK : bestCodeword;
        CorrectedBinaryMessage corrected = new CorrectedBinaryMessage(ROW_LENGTH * 2);

        for(int x = 0; x < ROW_LENGTH; x++)
        {
            corrected.set(x, (bestCodeword & (1 << x)) != 0);
            corrected.set(x + ROW_LENGTH, (second & (1 << x)) != 0);
        }

        corrected.setCorrectedBitCount(bestDistance);
        return corrected;
    }

    /**
     * Performs deinterleave of the interleaved message.
     *
     * @param original to deinterleave
     * @return deinterleaved message
     */
    public static CorrectedBinaryMessage deinterleave(CorrectedBinaryMessage original)
    {
        CorrectedBinaryMessage delinterleaved = new CorrectedBinaryMessage(32);
        for(int x = 0; x < 32; x++)
        {
            if(original.get(x))
            {
                delinterleaved.set(DEINTERLEAVE[x]);
            }
        }

        return delinterleaved;
    }

    public static void main(String[] args)
    {
        String[] msgs = new String[]{"05030A03", "35003A00", "1C6D2C9E"};

        for(String msg : msgs)
        {
            CorrectedBinaryMessage original = new CorrectedBinaryMessage(BinaryMessage.loadHex(msg));
            System.out.println("ORIGINAL: " + original.toHexString());
            CorrectedBinaryMessage decoded = decodeReverseChannel(original);
            System.out.println(" DECODED: " + (decoded != null ? decoded.toHexString() + " corrected bits:" +
                    decoded.getCorrectedBitCount() : "uncorrectable"));
            System.out.println("-------------------------");
        }
    }
}
