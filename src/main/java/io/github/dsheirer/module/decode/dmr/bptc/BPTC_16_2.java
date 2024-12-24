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

/**
 * Block Product Turbo Code 16/2 for decoding a DMR Voice Frame F payload from the 32-bit EMB field.
 * <p>
 * See ETSI TS 102 361-1 B.2.2.1 and B.2.2.2
 */
public class BPTC_16_2
{
    private static final int[] DEINTERLEAVE = new int[]{0, 24, 1, 25, 2, 26, 3, 27, 4, 28, 5, 29, 6, 30, 7, 31, 8, 16,
            9, 17, 10, 18, 11, 19, 12, 20, 13, 21, 14, 22, 15, 23};

    /**
     * Unscramble and perform FEC checks per paragraph B.2.2.1 for Non-Reverse Channel Single Burst
     *
     * @param message with 32 interleaved bits.
     * @return descrambled and error checked message or null if the process fails or there are too many errors.
     */
    public static CorrectedBinaryMessage decodeShortBurst(CorrectedBinaryMessage message)
    {
        CorrectedBinaryMessage deinterleaved = deinterleave(message);
        int fec = Hamming16.checkAndCorrect(deinterleaved, 0);

        if(fec == 2) //0 or 1 is good, 2 = uncorrectable errors
        {
            return null;
        }

        //Check for even parity.  Bits 0-15 should be the same as bits 16-31.
        for(int x = 0; x < 16; x++)
        {
            if(deinterleaved.get(x) ^ deinterleaved.get(x + 16))
            {
                return null;
            }
        }

        return deinterleaved;
    }

    /**
     * Unscramble and perform FEC checks per paragraph B.2.2.2 for Reverse Channel Single Burst
     *
     * @param binaryMessage with 32 interleaved bits.
     * @return descrambled and error checked message or null if the process fails or there are too many errors.
     */
    public static CorrectedBinaryMessage decodeReverseChannel(CorrectedBinaryMessage message)
    {
        CorrectedBinaryMessage deinterleaved = deinterleave(message);
        System.out.println(" DEINTER: " + deinterleaved.toHexString());
        int fec = Hamming16.checkAndCorrect(deinterleaved, 0);
        System.out.println(" DECODED: " + deinterleaved.toHexString());
        System.out.println("FEC:" + fec);
        if(fec == 2) //0 or 1 is good, 2 = uncorrectable errors
        {
            return null;
        }

        //Check for odd parity.  Bits 0-15 should be opposite of bits 16-31.
        for(int x = 0; x < 16; x++)
        {
            if(deinterleaved.get(x) == deinterleaved.get(x + 16))
            {
                return null;
            }
        }

        return deinterleaved;
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
            System.out.println("-------------------------");
        }
    }
}
