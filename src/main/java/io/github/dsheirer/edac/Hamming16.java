/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

/**
 * Hamming(16,11,4) Error detection and correction utility.  This code is capable of correcting single-bit errors and
 * detecting (but not correcting) double-bit errors.
 */
public class Hamming16 implements IHamming
{
    private static int[] CHECKSUMS = new int[]{0x13, 0x1A, 0x1F, 0x1C, 0x0E, 0x15, 0x0B, 0x16, 0x19, 0x0D, 0x07, 0x10,
            0x08, 0x04, 0x02, 0x01};

    /**
     * Calculates the bit error index of the Hamming(16,11,4) protected word that is contained in the binary message
     * starting at the specified offset.
     *
     * @param message containing a Hamming protected word
     * @param offset to the start of the protected word
     * @return message index for an error bit or -1 if no errors are detected or 1000 if two bit errors are detected.
     */
    public int getErrorIndex(BinaryMessage message, int offset)
    {
        int syndrome = getSyndrome(message, offset);

        if(syndrome == 0)
        {
            return NO_ERRORS;
        }

        //If the syndrome indicates the error is in the final parity bit position, and we already have odd parity, then
        //flag it as invalid for multiple errors.
        if(syndrome == 1 && message.getSubMessage(offset, offset + 16).cardinality() % 2 == 1) //check final parity bit
        {
            return MULTIPLE_ERRORS;
        }

        for(int index = 0; index < CHECKSUMS.length; index++)
        {
            if(syndrome == CHECKSUMS[index])
            {
                return index + offset;
            }
        }

        return MULTIPLE_ERRORS;
    }


    /**
     * Performs error detection and correction of any single-bit errors and detection of any double-bit errors (SECDED)
     *
     * @param frame - binary frame containing a hamming(16,11,5) protected field
     * @param startIndex - offset to the first bit of the field
     * @return - 0 = no errors
     * 1 = a single-bit error was detected and corrected
     * 2 = two or more errors detected - no corrections made
     */
    public static int checkAndCorrect(BinaryMessage frame, int startIndex)
    {
        int syndrome = getSyndrome(frame, startIndex);

        if (syndrome == 0)
        {
            return 0;
        }

        for(int index = 0; index < CHECKSUMS.length; index++)
        {
            if(CHECKSUMS[index] == syndrome)
            {
                frame.flip(startIndex + index);
                return 1;
            }
        }

        return 2;
    }

    /**
     * Calculates the checksum from the transmitted data bits.
     *
     * @param frame - frame containing hamming protected word
     * @param startIndex - start bit index of the hamming protected word
     * @return calculated checksum value
     */
    private static int calculateChecksum(BinaryMessage frame, int startIndex)
    {
        int calculated = 0; //Starting value

        /* Iterate the set bits and XOR running checksum with lookup value */
        for(int i = frame.nextSetBit(startIndex); i >= startIndex && i < startIndex + 11; i = frame.nextSetBit(i + 1))
        {
            calculated ^= CHECKSUMS[i - startIndex];
        }

        return calculated;
    }

    /**
     * Calculates the syndrome - xor of the calculated checksum and the actual
     * checksum.
     *
     * @param frame - binary frame containing a hamming(16,11,5) protected word
     * @param startIndex - of bit 0 of the hamming protected word
     * @return - 0 (no errors) or 1 (single bit error corrected)
     */
    public static int getSyndrome(BinaryMessage frame, int startIndex)
    {
        int calculated = calculateChecksum(frame, startIndex);
        int checksum = frame.getInt(startIndex + 11, startIndex + 15);
        return (checksum ^ calculated);
    }
}
