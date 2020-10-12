package io.github.dsheirer.edac;

import io.github.dsheirer.bits.BinaryMessage;

/**
 * Hamming(17,12,3) Error detection and correction utility.
 */
public class Hamming17
{
    private static int[] CHECKSUMS = new int[]{0x1B, 0x1F, 0x1D, 0x1C, 0x0E, 0x07, 0x11, 0x1A, 0x0D, 0x14, 0x0A, 0x05};

    /**
     * Performs error detection and correction of any single-bit errors and detection of any double-bit errors (SECDED)
     *
     * @param frame - binary frame containing a hamming(17,12,5) protected field
     * @param startIndex - offset to the first bit of the field
     * @return - 0 = no errors
     * 1 = a single-bit error was detected and corrected
     * 2 = two or more errors detected - no corrections made
     */
    public static int checkAndCorrect(BinaryMessage frame, int startIndex)
    {
        int syndrome = getSyndrome(frame, startIndex);

        switch(syndrome)
        {
            case 0:
                return 0;
            case 1:
                frame.flip(startIndex + 16); //Parity 1
                return 1;
            case 2:
                frame.flip(startIndex + 15); //Parity 2
                return 1;
            case 3:
                frame.flip(startIndex + 11); //Data 1
                return 1;
            case 4:
                frame.flip(startIndex + 14); //Parity 4
                return 1;
            case 5:
                frame.flip(startIndex + 10); //Data 2
                return 1;
            case 6:
                frame.flip(startIndex + 9); //Data 3
                return 1;
            case 7:
                frame.flip(startIndex + 8); //Data 4
                return 1;
            case 8:
                frame.flip(startIndex + 13); //Parity 8
                return 1;
            case 9:
                frame.flip(startIndex + 7); //Data 5
                return 1;
            case 10:
                frame.flip(startIndex + 6); //Data 6
                return 1;
            case 11:
                frame.flip(startIndex + 5); //Data 7
                return 1;
            case 12:
                frame.flip(startIndex + 4); //Data 8
                return 1;
            case 13:
                frame.flip(startIndex + 3); //Data 9
                return 1;
            case 14:
                frame.flip(startIndex + 2); //Data 10
                return 1;
            case 15:
                frame.flip(startIndex + 1); //Data 11
                return 1;
            case 16:
                frame.flip(startIndex + 12); //Parity 16
                return 1;
            case 17:
                frame.flip(startIndex); //Data 12
                return 1;
            default:
                return 2;
        }
    }

    /**
     * Calculates the checksum (Parity 16,8,4,2,1) for data (12 <> 1 ) bits.
     *
     * @param frame - frame containing hamming protected word
     * @param startIndex - start bit index of the hamming protected word
     * @return parity value, 0 - 16
     */
    private static int calculateChecksum(BinaryMessage frame, int startIndex)
    {
        int calculated = 0; //Starting value

        /* Iterate the set bits and XOR running checksum with lookup value */
        for(int i = frame.nextSetBit(startIndex);
            i >= startIndex && i < startIndex + 12;
            i = frame.nextSetBit(i + 1))
        {
            calculated ^= CHECKSUMS[i - startIndex];
        }

        return calculated;
    }

    /**
     * Calculates the syndrome - xor of the calculated checksum and the actual
     * checksum.
     *
     * @param frame - binary frame containing a hamming(17,12,5) protected word
     * @param startIndex - of bit 0 of the hamming protected word
     * @return - 0 (no errors) or 1 (single bit error corrected)
     */
    private static int getSyndrome(BinaryMessage frame, int startIndex)
    {
        int calculated = calculateChecksum(frame, startIndex);

        int checksum = frame.getInt(startIndex + 12, startIndex + 16);

        return (checksum ^ calculated);
    }
}
