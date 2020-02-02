package io.github.dsheirer.edac;

import io.github.dsheirer.bits.BinaryMessage;

/**
 * Implements Hamming(13,9,3) Error Detection algorithm
 */
public class Hamming13
{
    //DMR Checksums from generator matrix TS 102 361-1 Table B.14
    private static int[] CHECKSUMS = new int[]{0xF, 0xE, 0x7, 0xA, 0x5, 0XB, 0xC, 0x6, 0x3};
    private static int[] ERROR_INDEX = new int[]{-1, 12, 11, 8, 10, 4, 7, 2, 9, -1, 3, 5, 6, -1, 1, 0, -1};

    /**
     * Calculates the bit error index of the Hamming(13,9,3) protected word that is contained in the binary message
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

        return -1;
    }

    /**
     * Calculates the bit error index of the Hamming(13,9,3) protected word that is contained in the binary message
     * at the specified indices.
     *
     * @param message containing the Hamming protected word
     * @param indices to the word
     * @return index of error bit or -1 if no errors are detected
     */
    public static int getErrorIndex(BinaryMessage message, int[] indices)
    {
        int syndrome = getSyndrome(message, indices);

        if(syndrome > 0)
        {
            int errorIndex = ERROR_INDEX[syndrome];

            if(errorIndex >= 0 && errorIndex < indices.length)
            {
                return indices[errorIndex];
            }
            else
            {
                return 10000 + errorIndex;
            }
        }

        return -1;
    }

    /**
     * Calculates the parity checksum (Parity 8,4,2,1) for data (9 <> 1 ) bits.
     *
     * @param message containing Hamming(15) protected word
     * @param offset to the Hamming protected word
     * @return parity value, 0 - 15
     */
    private static int calculateChecksum(BinaryMessage message, int offset)
    {
        int calculated = 0; //Starting value

        /* Iterate the set bits and XOR running checksum with lookup value */
        for(int i = message.nextSetBit(offset); i >= offset && i < offset + 9; i = message.nextSetBit(i + 1))
        {
            calculated ^= CHECKSUMS[i - offset];
        }

        return calculated;
    }

    private static int calculateChecksum(BinaryMessage message, int[] indices)
    {
        int calculated = 0; //Starting value

        for(int x = 0; x < 9; x++)
        {
            if(message.get(indices[x]))
            {
                calculated ^= CHECKSUMS[x];
            }
        }

        return calculated;
    }

    private static int getSyndrome(BinaryMessage message, int[] indices)
    {
        int calculated = calculateChecksum(message, indices);

        int checksum = 0;
        for(int x = 9; x < 13; x++)
        {
            checksum = Integer.rotateLeft(checksum, 1);

            if(message.get(indices[x]))
            {
                checksum++;
            }
        }

        return calculated ^ checksum;
    }

    /**
     * Calculates the syndrome as the xor of the calculated checksum and the actual checksum.
     *
     * @param message containing a hamming(15,11,3) protected word
     * @param offset to bit 0 of the hamming protected word
     * @return syndrome that can be used with the ERROR_INDEX error to find the index of the bit position error
     */
    private static int getSyndrome(BinaryMessage message, int offset)
    {
        int calculated = calculateChecksum(message, offset);
        int checksum = message.getInt(offset + 9, offset + 12);
        return (checksum ^ calculated);
    }
}
