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

package io.github.dsheirer.module.decode.dmr.message.voice;

import io.github.dsheirer.bits.BinaryMessage;

/**
 * Error detection for the three AMBE+2 (3600 x 2450) voice frames carried in a DMR voice burst.
 *
 * Each 72-bit voice frame carries an extended Golay(24,12) protected C0 vector and a Golay(23,12) protected C1 vector
 * that is scrambled with a pseudo-random sequence seeded by the 12 data bits of C0.  The C2 and C3 vectors are not
 * protected.  Golay(23,12) is a perfect code where every 23-bit word decodes to a codeword within 3 bit errors, so a
 * frame is only considered valid when the combined C0 and C1 error count is low, which random (ie noise) bits rarely
 * achieve.
 *
 * Vector bit indices and the C1 scrambling sequence are from the JMBE AMBEFrame decoder.  Burst bit indices of each
 * voice frame match VoiceMessage.getAMBEFrames(): frames 1 and 3 are contiguous and frame 2 is split around the 48-bit
 * sync/embedded signalling field in the middle of the burst.
 */
public class AMBEFrameValidator
{
    /**
     * Maximum combined C0 and C1 bit errors for a valid voice frame.  Random 47-bit sequences have a combined error
     * count of 2 or less with a probability of ~1.3e-4.
     */
    public static final int MAX_ERRORS = 2;

    private static final int[] VECTOR_C0 = {0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 52, 56, 60, 64, 68, 1, 5,
            9, 13, 17, 21};
    private static final int[] VECTOR_C1 = {25, 29, 33, 37, 41, 45, 49, 53, 57, 61, 65, 69, 2, 6, 10, 14, 18, 22, 26,
            30, 34, 38, 42};
    private static final int[] FRAME_OFFSETS = {24, 96, 216};
    private static final int GOLAY_23_GENERATOR = 0xC75;
    private static final int[][] C0_INDICES = new int[3][VECTOR_C0.length];
    private static final int[][] C1_INDICES = new int[3][VECTOR_C1.length];
    //Minimum weight error pattern for each Golay(23,12) syndrome (perfect code: all syndromes have weight <= 3)
    private static final int[] ERROR_PATTERNS = new int[2048];
    //C1 descrambling sequence for each 12-bit C0 data (u0) seed value, MSB first
    private static final int[] SCRAMBLE_SEQUENCES = new int[4096];

    static
    {
        for(int frame = 0; frame < 3; frame++)
        {
            for(int x = 0; x < VECTOR_C0.length; x++)
            {
                C0_INDICES[frame][x] = getBurstIndex(frame, VECTOR_C0[x]);
            }

            for(int x = 0; x < VECTOR_C1.length; x++)
            {
                C1_INDICES[frame][x] = getBurstIndex(frame, VECTOR_C1[x]);
            }
        }

        //Golay(23,12) is perfect: the 2048 error patterns of weight 0 - 3 map one-to-one onto the 2048 syndromes.
        for(int i = 0; i < 23; i++)
        {
            ERROR_PATTERNS[getSyndrome(1 << i)] = 1 << i;

            for(int j = i + 1; j < 23; j++)
            {
                ERROR_PATTERNS[getSyndrome((1 << i) | (1 << j))] = (1 << i) | (1 << j);

                for(int k = j + 1; k < 23; k++)
                {
                    int pattern = (1 << i) | (1 << j) | (1 << k);
                    ERROR_PATTERNS[getSyndrome(pattern)] = pattern;
                }
            }
        }

        for(int seed = 0; seed < 4096; seed++)
        {
            int pr = 16 * seed;
            int sequence = 0;

            for(int x = 0; x < 23; x++)
            {
                pr = (173 * pr + 13849) % 65536;
                sequence = (sequence << 1) | (pr >= 32768 ? 1 : 0);
            }

            SCRAMBLE_SEQUENCES[seed] = sequence;
        }
    }

    private AMBEFrameValidator()
    {
    }

    /**
     * Burst bit index for the voice frame bit index.
     * @param frame index 0-2
     * @param bit index 0-71 within the (interleaved) voice frame
     * @return bit index within the 288-bit burst
     */
    private static int getBurstIndex(int frame, int bit)
    {
        //Frame 2 is split around the 48-bit sync/embedded signalling field (burst bits 132 - 179)
        if(frame == 1 && bit >= 36)
        {
            return FRAME_OFFSETS[frame] + bit + 48;
        }

        return FRAME_OFFSETS[frame] + bit;
    }

    /**
     * Golay(23,12) syndrome for the 23-bit word where the MSB (bit 22) is the first codeword bit.
     */
    private static int getSyndrome(int word)
    {
        for(int bit = 22; bit >= 11; bit--)
        {
            if((word & (1 << bit)) != 0)
            {
                word ^= GOLAY_23_GENERATOR << (bit - 11);
            }
        }

        return word;
    }

    /**
     * Extracts the indexed bits from the message as an integer value, with the first index as the MSB.
     */
    private static int getValue(BinaryMessage message, int[] indices)
    {
        int value = 0;

        for(int index : indices)
        {
            value = (value << 1) | (message.get(index) ? 1 : 0);
        }

        return value;
    }

    /**
     * Combined C0 and C1 bit error count for the voice frame, where C0 is decoded first so that the corrected C0 data
     * bits can seed the C1 descrambling sequence.  C0 errors include the extended Golay parity bit, so an even weight
     * C0 error pattern (ie 4 errors) that Golay(23,12) mis-corrects is counted as 4 errors.
     * @param message containing the 288-bit voice burst
     * @param frame index 0-2
     * @return combined C0 and C1 bit error count.
     */
    public static int getErrorCount(BinaryMessage message, int frame)
    {
        int c0 = getValue(message, C0_INDICES[frame]);
        int golay = c0 >>> 1;
        int pattern = ERROR_PATTERNS[getSyndrome(golay)];
        int corrected = golay ^ pattern;
        int errors = Integer.bitCount(pattern);

        //Extended Golay(24,12) parity bit provides even parity across the 24-bit codeword
        if(((Integer.bitCount(corrected) + (c0 & 1)) & 1) != 0)
        {
            errors++;
        }

        //Short circuit when C0 alone exceeds the max error count.
        if(errors > MAX_ERRORS)
        {
            return errors;
        }

        int c1 = getValue(message, C1_INDICES[frame]) ^ SCRAMBLE_SEQUENCES[corrected >>> 11];
        return errors + Integer.bitCount(ERROR_PATTERNS[getSyndrome(c1)]);
    }

    /**
     * Indicates if at least one of the three voice frames in the burst is valid, checking each frame in turn until a
     * valid frame is found.
     * @param message containing the 288-bit voice burst
     * @return true if at least one voice frame has a combined C0 and C1 error count within the max error count.
     */
    public static boolean isValid(BinaryMessage message)
    {
        for(int frame = 0; frame < 3; frame++)
        {
            if(getErrorCount(message, frame) <= MAX_ERRORS)
            {
                return true;
            }
        }

        return false;
    }
}
