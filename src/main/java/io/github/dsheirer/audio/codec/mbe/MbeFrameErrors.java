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

package io.github.dsheirer.audio.codec.mbe;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.Golay23;
import io.github.dsheirer.edac.Golay24;

/**
 * Counts bit errors in the first protected block (c0) of an IMBE or AMBE voice frame. JMBE checks the same block
 * before deciding to play, repeat or mute a frame, so a frame with more than one error here is one a listener may
 * hear as warble or static. The tables match JMBE's IMBEInterleave and AMBEFrame.
 */
public final class MbeFrameErrors
{
    /** More errors than the code can correct. */
    public static final int UNCORRECTABLE = 4;

    private static final int[] IMBE_DEINTERLEAVE = {0, 24, 48, 72, 96, 120, 25, 1, 73, 49, 121, 97, 2, 26, 50, 74, 98,
        122, 27, 3, 75, 51, 123, 99, 4, 28, 52, 76, 100, 124, 29, 5, 77, 53, 125, 101, 6, 30, 54, 78, 102, 126, 31, 7,
        79, 55, 127, 103, 8, 32, 56, 80, 104, 128, 33, 9, 81, 57, 129, 105, 10, 34, 58, 82, 106, 130, 35, 11, 83, 59,
        131, 107, 12, 36, 60, 84, 108, 132, 37, 13, 85, 61, 133, 109, 14, 38, 62, 86, 110, 134, 39, 15, 87, 63, 135,
        111, 16, 40, 64, 88, 112, 136, 41, 17, 89, 65, 137, 113, 18, 42, 66, 90, 114, 138, 43, 19, 91, 67, 139, 115,
        20, 44, 68, 92, 116, 140, 45, 21, 93, 69, 141, 117, 22, 46, 70, 94, 118, 142, 47, 23, 95, 71, 143, 119};

    private static final int[] AMBE_C0 = {0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 52, 56, 60, 64, 68, 1, 5, 9,
        13, 17, 21};

    private MbeFrameErrors()
    {
    }

    /**
     * Bit errors in c0 of an 18-byte P25 phase 1 IMBE frame as handed to the codec, or -1 if the frame is malformed.
     */
    public static int imbe(byte[] frame)
    {
        if(frame == null || frame.length < 18)
        {
            return -1;
        }
        BinaryMessage interleaved = BinaryMessage.from(frame);
        BinaryMessage c0 = new BinaryMessage(23);
        for(int i = 0; i < IMBE_DEINTERLEAVE.length; i++)
        {
            if(IMBE_DEINTERLEAVE[i] < 23 && interleaved.get(i))
            {
                c0.set(IMBE_DEINTERLEAVE[i]);
            }
        }
        return Math.min(Golay23.checkAndCorrect(c0, 0), UNCORRECTABLE);
    }

    /**
     * Bit errors in c0 of a 9-byte AMBE frame (P25 phase 2, DMR) as handed to the codec, or -1 if malformed.
     */
    public static int ambe(byte[] frame)
    {
        if(frame == null || frame.length < 9)
        {
            return -1;
        }
        BinaryMessage raw = BinaryMessage.from(frame);
        CorrectedBinaryMessage c0 = new CorrectedBinaryMessage(24);
        for(int i = 0; i < AMBE_C0.length; i++)
        {
            if(raw.get(AMBE_C0[i]))
            {
                c0.set(i);
            }
        }
        int before = c0.getInt(0, 23);
        if(Golay24.checkAndCorrect(c0, 0) == 2)
        {
            return UNCORRECTABLE;
        }
        return Integer.bitCount(before ^ c0.getInt(0, 23));
    }
}
