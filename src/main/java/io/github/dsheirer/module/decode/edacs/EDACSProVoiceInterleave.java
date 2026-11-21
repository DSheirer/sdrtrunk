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

// Reference: DSD-FME edacs-fme.c provoice.c / provoice_const.h (lwvmobile/edacs-fm).
// Algorithm: ProVoice carries 4 IMBE 7100 voice frames per outbound frame
// at 9600 baud. The 144 bits of each IMBE frame are interleaved with a
// neighbouring IMBE frame using a 7x24 bit-grid schedule, with two bits
// per grid cell. The pW/pX tables below are the wire-position -> grid-cell
// mapping; the DEINTERLEAVE table is the IMBE 7100 codec's grid-cell ->
// codec-input mapping (re-implemented from the IMBE 7100 deinterleave
// schedule, identical to jmbe.codec.imbe.IMBEInterleave).
package io.github.dsheirer.module.decode.edacs;

/**
 * ProVoice IMBE 7100 interleave/deinterleave. Each ProVoice outbound
 * voice frame carries 4 IMBE 7100 voice frames (20 ms each) interleaved
 * two-at-a-time across a 7x24 bit grid. The 142 active grid cells per
 * IMBE frame are mapped from the wire via the {@code pW}/{@code pX}
 * tables, then the 144-bit codec input is read out via
 * {@code DEINTERLEAVE}.
 */
public class EDACSProVoiceInterleave
{
    /**
     * Row index in the 7x24 IMBE bit grid for each of the 142 wire
     * bits that carry one IMBE 7100 frame. Re-implemented from the
     * ProVoice interleave schedule documented in
     * {@code provoice_const.h}.
     */
    static final int[] pW = new int[] {
        0, 1, 2, 3, 4, 6,
        0, 1, 2, 3, 4, 6,
        0, 1, 2, 3, 4, 6,
        0, 1, 2, 3, 5, 6,
        0, 1, 2, 3, 5, 6,
        0, 1, 2, 3, 5, 6,
        0, 1, 3, 4, 5, 6,
        1, 2, 3, 4, 5, 6,
        0, 1, 2, 3, 4, 6,
        0, 1, 2, 3, 4, 6,
        0, 1, 2, 3, 4, 6,
        0, 1, 2, 3, 5, 6,
        0, 1, 2, 3, 5, 6,
        0, 1, 2, 3, 5, 6,
        1, 2, 3, 4, 5, 6,
        1, 2, 3, 4, 5,
        0, 1, 2, 3, 4, 6,
        0, 1, 2, 3, 4, 6,
        0, 1, 2, 3, 5, 6,
        0, 1, 2, 3, 5, 6,
        0, 1, 2, 3, 5, 6,
        0, 1, 2, 4, 5, 6,
        1, 2, 3, 4, 5, 6,
        1, 2, 3, 4, 6
    };

    /** Column index in the 7x24 IMBE bit grid for each of the 142 wire bits. */
    static final int[] pX = new int[] {
        18, 18, 17, 16, 7, 21,
        15, 15, 14, 13, 4, 18,
        12, 12, 11, 10, 1, 15,
        9, 9, 8, 7, 13, 12,
        6, 6, 5, 4, 10, 9,
        3, 3, 2, 1, 7, 6,
        0, 0, 22, 13, 4, 3,
        21, 20, 19, 10, 1, 0,
        17, 17, 16, 15, 6, 20,
        14, 14, 13, 12, 3, 17,
        11, 11, 10, 9, 0, 14,
        8, 8, 7, 6, 12, 11,
        5, 5, 4, 3, 9, 8,
        2, 2, 1, 0, 6, 5,
        23, 22, 21, 12, 3, 2,
        20, 19, 18, 9, 0,
        16, 16, 15, 14, 5, 19,
        13, 13, 12, 11, 2, 16,
        10, 10, 9, 8, 14, 13,
        7, 7, 6, 5, 11, 10,
        4, 4, 3, 2, 8, 7,
        1, 1, 0, 14, 5, 4,
        22, 21, 20, 11, 2, 1,
        19, 18, 17, 8, 22
    };

    /**
     * IMBE 7100 deinterleave schedule: for each input position i in
     * 0..143, the corresponding output position is DEINTERLEAVE[i]. This
     * is identical to the table in {@code jmbe.codec.imbe.IMBEInterleave};
     * the JMBE table is package-private so we keep a copy here.
     */
    static final int[] DEINTERLEAVE = new int[] {
        0, 24, 48, 72, 96, 120, 25,
        1, 73, 49, 121, 97, 2, 26, 50, 74, 98, 122, 27, 3, 75, 51, 123, 99,
        4, 28, 52, 76, 100, 124, 29, 5, 77, 53, 125, 101, 6, 30, 54, 78, 102, 126,
        31, 7, 79, 55, 127, 103, 8, 32, 56, 80, 104, 128, 33, 9, 81, 57, 129, 105,
        10, 34, 58, 82, 106, 130, 35, 11, 83, 59, 131, 107, 12, 36, 60, 84, 108, 132,
        37, 13, 85, 61, 133, 109, 14, 38, 62, 86, 110, 134, 39, 15, 87, 63, 135, 111,
        16, 40, 64, 88, 112, 136, 41, 17, 89, 65, 137, 113, 18, 42, 66, 90, 114, 138,
        43, 19, 91, 67, 139, 115, 20, 44, 68, 92, 116, 140, 45, 21, 93, 69, 141, 117,
        22, 46, 70, 94, 118, 142, 47, 23, 95, 71, 143, 119
    };

    /**
     * Convert 142 wire bits (one IMBE 7100 frame) to an 18-byte IMBE
     * codec input. The codec expects the bits in codec-input order, which
     * is the inverse of the DEINTERLEAVE table applied to a 7x24 grid
     * where the 142 active cells are filled by the {@code pW}/{@code pX}
     * tables and the remaining 26 cells are zero.
     *
     * @param wireBits 142 wire bits in the order they arrive on the
     *                  ProVoice frame, with bit 0 = first bit of this
     *                  IMBE frame on the wire.
     * @return 18-byte IMBE 7100 frame suitable for
     *         {@code jmbe.codec.imbe.IMBEAudioCodec.getAudio()}.
     */
    public static byte[] toImbeFrame(boolean[] wireBits)
    {
        if(wireBits.length != 142)
        {
            throw new IllegalArgumentException("ProVoice IMBE frame requires 142 wire bits, got " + wireBits.length);
        }

        // Fill the 7x24 IMBE bit grid with the 142 wire bits at the
        // pW/pX positions; the remaining 26 cells stay 0.
        int[][] grid = new int[7][24];
        for(int i = 0; i < 142; i++)
        {
            grid[pW[i]][pX[i]] = wireBits[i] ? 1 : 0;
        }

        return toImbeFrame(grid);
    }

    /**
     * Converts a DSD-FME/mbelib-style 7x24 IMBE 7100 bit grid into the
     * 18-byte interleaved frame representation expected by JMBE.
     */
    public static byte[] toImbeFrame(int[][] grid)
    {
        if(grid.length != 7)
        {
            throw new IllegalArgumentException("IMBE grid requires 7 rows");
        }

        for(int row = 0; row < 7; row++)
        {
            if(grid[row].length != 24)
            {
                throw new IllegalArgumentException("IMBE grid row " + row + " requires 24 columns");
            }
        }

        // Build the 144-bit codec input. For each i, position
        // DEINTERLEAVE[i] in the 7x24 grid contains the bit the codec
        // expects at input position i. The 2 grid cells at positions
        // 142-167 are not in DEINTERLEAVE so they are never read; the 2
        // cells at the DEINTERLEAVE positions that fall outside pW/pX's
        // 142-entry range are also 0 (which is what the codec expects
        // for those bits).
        boolean[] codecInput = new boolean[144];
        for(int i = 0; i < 144; i++)
        {
            int pos = DEINTERLEAVE[i];
            int row = pos / 24;
            int col = pos % 24;
            codecInput[i] = (grid[row][col] != 0);
        }

        // Pack the 144 bits into 18 bytes, MSB-first, in the byte order
        // jmbe.binary.BinaryFrame.fromBytes(..., LITTLE_ENDIAN) expects.
        byte[] result = new byte[18];
        for(int i = 0; i < 144; i++)
        {
            if(codecInput[i])
            {
                result[i / 8] |= (byte)(0x80 >>> (i % 8));
            }
        }
        return result;
    }

    /**
     * Convenience: build the IMBE 7100 input as a {@link BinaryFrame} for
     * direct use with JMBE's IMBEAudioCodec.
     */
    public static byte[] toImbeFrameBytes(boolean[] wireBits)
    {
        return toImbeFrame(wireBits);
    }
}
