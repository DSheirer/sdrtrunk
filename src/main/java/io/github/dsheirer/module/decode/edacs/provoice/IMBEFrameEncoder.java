/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.edacs.provoice;

/**
 * Builds a P25 Phase 1 IMBE voice frame (144 bits, 18 bytes) from 88 parameter bits, so that
 * the JMBE IMBE codec - which decodes P25 voice - can synthesize ProVoice audio.
 *
 * Layout (TIA-102.BABA): vectors u0-u3 (12 bits) as Golay (23,12) codewords, u4-u6 (11 bits) as Hamming (15,11)
 * codewords, u7 (7 bits) plain; c1-c6 scrambled with the sequence seeded from u0; then interleaved.  The parity
 * values, scrambler and interleave are the inverse of what JMBE's IMBEFrame checks, and a test confirms that JMBE reads
 * back exactly these bits.
 */
public class IMBEFrameEncoder
{
    private static final int[] GOLAY_CHECKSUMS = {0x63A, 0x31D, 0x7B4, 0x3DA, 0x1ED, 0x6CC, 0x366, 0x1B3, 0x6E3, 0x54B,
        0x49F, 0x475};
    private static final int[] HAMMING_CHECKSUMS = {0xF, 0xE, 0xD, 0xC, 0xB, 0xA, 0x9, 0x7, 0x6, 0x5, 0x3};
    private static final int[] DEINTERLEAVE = {0, 24, 48, 72, 96, 120, 25, 1, 73, 49, 121, 97, 2, 26, 50, 74, 98, 122,
        27, 3, 75, 51, 123, 99, 4, 28, 52, 76, 100, 124, 29, 5, 77, 53, 125, 101, 6, 30, 54, 78, 102, 126, 31, 7, 79, 55,
        127, 103, 8, 32, 56, 80, 104, 128, 33, 9, 81, 57, 129, 105, 10, 34, 58, 82, 106, 130, 35, 11, 83, 59, 131, 107,
        12, 36, 60, 84, 108, 132, 37, 13, 85, 61, 133, 109, 14, 38, 62, 86, 110, 134, 39, 15, 87, 63, 135, 111, 16, 40,
        64, 88, 112, 136, 41, 17, 89, 65, 137, 113, 18, 42, 66, 90, 114, 138, 43, 19, 91, 67, 139, 115, 20, 44, 68, 92,
        116, 140, 45, 21, 93, 69, 141, 117, 22, 46, 70, 94, 118, 142, 47, 23, 95, 71, 143, 119};
    /** Start and length of the parameter bits of each codeword in the de-interleaved frame */
    private static final int[][] VECTORS = {{0, 12}, {23, 12}, {46, 12}, {69, 12}, {92, 11}, {107, 11}, {122, 11},
        {137, 7}};

    private IMBEFrameEncoder()
    {
    }

    /**
     * De-interleaved, unscrambled frame with parity - what JMBE holds after its error correction.
     * @param parameters 88 bits (0/1)
     * @return 144 bits (0/1)
     */
    static int[] codewords(int[] parameters)
    {
        int[] frame = new int[144];
        int n = 0;

        for(int[] vector : VECTORS)
        {
            for(int i = 0; i < vector[1]; i++)
            {
                frame[vector[0] + i] = parameters[n++];
            }
        }

        for(int c = 0; c < 4; c++)
        {
            int offset = c * 23;
            int parity = 0;

            for(int i = 0; i < 12; i++)
            {
                if(frame[offset + i] == 1)
                {
                    parity ^= GOLAY_CHECKSUMS[i];
                }
            }

            for(int k = 0; k < 11; k++)
            {
                frame[offset + 12 + k] = (parity >> (10 - k)) & 1;
            }
        }

        for(int c = 0; c < 3; c++)
        {
            int offset = 92 + c * 15;
            int parity = 0;

            for(int i = 0; i < 11; i++)
            {
                if(frame[offset + i] == 1)
                {
                    parity ^= HAMMING_CHECKSUMS[i];
                }
            }

            frame[offset + 11] = (parity >> 3) & 1;
            frame[offset + 12] = (parity >> 2) & 1;
            frame[offset + 13] = (parity >> 1) & 1;
            frame[offset + 14] = parity & 1;
        }

        return frame;
    }

    /**
     * @param parameters 88 bits (0/1) in P25 order
     * @return 18-byte P25 IMBE frame
     */
    public static byte[] encode(int[] parameters)
    {
        int[] frame = codewords(parameters);

        int seed = 0;
        for(int i = 0; i < 12; i++)
        {
            seed = (seed << 1) | frame[i];
        }

        int pr = 16 * seed;
        for(int x = 0; x < 114; x++)
        {
            pr = (173 * pr + 13849) % 65536;

            if(pr >= 32768)
            {
                frame[x + 23] ^= 1;
            }
        }

        byte[] bytes = new byte[18];

        for(int i = 0; i < 144; i++)
        {
            if(frame[DEINTERLEAVE[i]] == 1)
            {
                bytes[i / 8] |= (byte)(0x80 >> (i % 8));
            }
        }

        return bytes;
    }
}
