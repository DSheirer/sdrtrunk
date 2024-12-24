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

package io.github.dsheirer.edac;

import io.github.dsheirer.bits.BinaryMessage;

/**
 * Implements Hamming(15,11,3) Error Detection algorithm
 */
public class Hamming15
{
    //DMR Checksums from generator matrix TS 102 361-1 Table B.15
    private static int[] CHECKSUMS = new int[]{0x9, 0xD, 0xF, 0xE, 0x7, 0xA, 0x5, 0xB, 0xC, 0x6, 0x3, 0x8, 0x4, 0x2, 0x1};
    private static int[] ERROR_INDEX = new int[]{-1, 14, 13, 10, 12, 6, 9, 4, 11, 0, 5, 7, 8, 1, 3, 2};

    /**
     * Calculates the bit error index of the Hamming(15,11,3) protected word that is contained in the binary message
     * starting at the specified offset.
     *
     * @param message containing a Hamming protected word
     * @param offset to the start of the protected word
     * @return message index for an error bit or -1 if no errors are detected.
     */
    public static int getErrorIndex(BinaryMessage message, int offset)
    {
        int syndrome = getSyndrome(message, offset);

        if(syndrome > 0)
        {
            return offset + ERROR_INDEX[syndrome];
        }

        return IHamming.NO_ERRORS;
    }

    /**
     * Calculates the parity checksum (Parity 8,4,2,1) for data (11 <> 1 ) bits.
     *
     * @param message containing Hamming(15) protected word
     * @param offset to the Hamming protected word
     * @return parity value, 0 - 15
     */
    private static int calculateChecksum(BinaryMessage message, int offset)
    {
        int calculated = 0; //Starting value

        /* Iterate the set bits and XOR running checksum with lookup value */
        for(int i = message.nextSetBit(offset); i >= offset && i < offset + 11; i = message.nextSetBit(i + 1))
        {
            calculated ^= CHECKSUMS[i - offset];
        }

        return calculated;
    }

    /**
     * Calculates the syndrome as the xor of the calculated checksum and the actual checksum.
     *
     * @param message containing a hamming(15,11,3) protected word
     * @param offset to bit 0 of the hamming protected word
     * @return syndrome that can be used with the ERROR_INDEX error to find the index of the bit position error
     */
    public static int getSyndrome(BinaryMessage message, int offset)
    {
        int calculated = calculateChecksum(message, offset);
        int checksum = message.getInt(offset + 11, offset + 14);
        return (checksum ^ calculated);
    }
}
