/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Berlecamp-Massey decoder for Reed Solomon RS(12,9,4) protected messages using DMR generator polynomial
 */
public class ReedSolomon_12_9_4_DMR extends ReedSolomon_255_DMR
{
    private static final int[] FLC_CODEWORD_0 = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] FLC_CODEWORD_1 = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] FLC_CODEWORD_2 = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] FLC_CODEWORD_3 = {24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] FLC_CODEWORD_4 = {32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] FLC_CODEWORD_5 = {40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] FLC_CODEWORD_6 = {48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] FLC_CODEWORD_7 = {56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] FLC_CODEWORD_8 = {64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] FLC_PARITY_0 = {72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] FLC_PARITY_1 = {80, 81, 82, 83, 84, 85, 86, 87};
    private static final int[] FLC_PARITY_2 = {88, 89, 90, 91, 92, 93, 94, 95};

    /**
     * Constructs an instance
     */
    public ReedSolomon_12_9_4_DMR()
    {
        //Note: the RS(12,9,4) is a shortened form of RS(255,252,4) over GF(8)
        super(255, 252);
    }

    /**
     * Attempts to correct the Full Link Control message returning true if there were zero or one correctable errors
     * and returning false if there were two or more errors detected.
     *
     * Note: if one bit error was detected and corrected, the corrected bit count is set to 1 in the message
     *
     * @param message to be corrected.
     * @param crcMask as defined in the TS 102-361-1 Table B.21
     * @return true if the message was corrected.
     */
    public boolean correctFullLinkControl(CorrectedBinaryMessage message, int crcMask)
    {
        int[] input = new int[255];
        int[] output = new int[255];

        input[0] = message.getInt(FLC_PARITY_2) ^ crcMask;
        input[1] = message.getInt(FLC_PARITY_1) ^ crcMask;
        input[2] = message.getInt(FLC_PARITY_0) ^ crcMask;
        input[3] = message.getInt(FLC_CODEWORD_8);
        input[4] = message.getInt(FLC_CODEWORD_7);
        input[5] = message.getInt(FLC_CODEWORD_6);
        input[6] = message.getInt(FLC_CODEWORD_5);
        input[7] = message.getInt(FLC_CODEWORD_4);
        input[8] = message.getInt(FLC_CODEWORD_3);
        input[9] = message.getInt(FLC_CODEWORD_2);
        input[10] = message.getInt(FLC_CODEWORD_1);
        input[11] = message.getInt(FLC_CODEWORD_0);

        try
        {
            boolean irrecoverableErrors = decode(input, output);

            if(irrecoverableErrors)
            {
                return false;
            }
        }
        catch(Exception e)
        {
            return false;
        }

        int pointer = 0;

        for(int x = 11; x >= 0; x--)
        {
            if(output[x] != -1 && (output[x] != input[x]))
            {
                message.load(pointer, 8, output[x]);
                message.setCorrectedBitCount(1);
                return true;
            }

            pointer += 8;
        }

        return true;
    }
}
