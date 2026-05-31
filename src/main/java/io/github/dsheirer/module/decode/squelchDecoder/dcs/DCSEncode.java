package io.github.dsheirer.module.decode.squelchDecoder.dcs;

/**
 * converts a 9 - bit, 3 digit octal DCS code to a 23 bit binary word that can be utilized by the DCS decoder
 * for non-standard DCS codes.
 */

public class DCSEncode
{
    // Parity check generator polynomial: x^11 + x^9 + x^7 + x^6 + x^5 + x + 1 = 0xC75
    // In many implementations, it is written as 0xAE3 depending on bit ordering.
    // Here we use the standard cyclic code generator polynomial 0xC75 for MSB-first CRC layout.
    private static final int GENERATOR_POLY = 0xC75;



    public static int OctStr2Int (String octalString)
    {
        return Integer.parseInt(octalString, 8) | 0x800;
    }

    /**
     * Encodes a 12-bit message into a 23-bit systematic Golay codeword.
     *
     * @param message A 12-bit integer (0 to 4095).
     * @return A 23-bit encoded integer, that has been bit-reversed for use in DCSDecoder.
     */
    public static int encode(int message)
    {
        // Enforce 12-bit bound
        message &= 0xFFF;

        // Shift message to the left by 11 bits to leave room for parity
        int codeword = message << 11;

        // Compute the 11 parity bits using modulo-2 division (CRC style)
        int remainder = codeword;
        for (int i = 22; i >= 11; i--) {
            if ((remainder & (1 << i)) != 0) {
                remainder ^= (GENERATOR_POLY << (i - 11));
            }
        }

        // Combine the message bits and the calculated parity bits
        int combinedMessage = ((remainder & 0x7ff) << 12) | message;

        // reverse the order of the bits for DCSDecoder input
        int bitPosition = 0x400000;     // 23rd bit position set
        int reversedMessage = 0;
        for(int i = 0; i < 23; i++)
        {
            if((combinedMessage & 1) == 1)
            {
                reversedMessage = reversedMessage | bitPosition;
            }
            bitPosition = bitPosition >>> 1;
            combinedMessage = combinedMessage >> 1;
        }

        return reversedMessage;
    }
}
