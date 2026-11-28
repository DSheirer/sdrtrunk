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

package io.github.dsheirer.edac;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Tests that Golay(18,6,8) corrects up to three bit errors, reports four as uncorrectable and rejects a Golay(24,12)
 * correction that sets any of the 6 deleted bits.
 */
public class Golay18Test
{
    //(18,6,8) generator matrix rows in octal, TIA-102.BAAA-B Table 3
    private static final int[] GENERATOR = {0403315, 0201547, 0106706, 0045227, 0024476, 0014353};

    public static int encode(int data)
    {
        int codeword = 0;

        for(int row = 0; row < 6; row++)
        {
            if((data & (1 << (5 - row))) != 0)
            {
                codeword ^= GENERATOR[row];
            }
        }

        return codeword;
    }

    /**
     * Golay(24,12) encoding with the Golay24 checksums.
     */
    private static int encode24(int data)
    {
        int checksum = 0;

        for(int bit = 0; bit < 12; bit++)
        {
            if((data & (1 << (11 - bit))) != 0)
            {
                checksum ^= Golay24.CHECKSUMS[bit];
            }
        }

        int word23 = (data << 11) | checksum;
        return (word23 << 1) | (Integer.bitCount(word23) & 1);
    }

    /**
     * Decodes the word in a message with a bit on either side, so that a decode that writes outside the word fails.
     */
    private static int decode(int received, int[] decoded, int[] correctedBitCount)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(20);
        message.set(0);
        message.load(1, 18, received);
        message.set(19);
        int result = Golay18.checkAndCorrect(message, 1);
        assertEquals(1, message.getInt(0, 0), "bit before the word");
        assertEquals(1, message.getInt(19, 19), "bit after the word");
        decoded[0] = message.getInt(1, 18);
        correctedBitCount[0] = message.getCorrectedBitCount();
        return result;
    }

    @Test
    public void codewordsAreTheShortenedGolay24Codewords()
    {
        for(int data = 0; data < 64; data++)
        {
            assertEquals(encode24(data) & 0x3FFFF, encode(data), "data " + data);
        }
    }

    @Test
    public void correctsThreeErrorsAndDetectsFour()
    {
        int[] decoded = new int[1];
        int[] correctedBitCount = new int[1];

        for(int data = 0; data < 64; data++)
        {
            int codeword = encode(data);

            for(int errors = 0; errors < (1 << 18); errors++)
            {
                int weight = Integer.bitCount(errors);

                if(weight > 4)
                {
                    continue;
                }

                int received = codeword ^ errors;
                int result = decode(received, decoded, correctedBitCount);
                String label = String.format("data %02X errors %05X", data, errors);

                if(weight == 0)
                {
                    assertEquals(0, result, label);
                    assertEquals(codeword, decoded[0], label);
                    assertEquals(0, correctedBitCount[0], label);
                }
                else if(weight <= 3)
                {
                    assertEquals(1, result, label);
                    assertEquals(codeword, decoded[0], label);
                    assertEquals(weight, correctedBitCount[0], label);
                }
                else
                {
                    assertEquals(2, result, label);
                    assertEquals(received, decoded[0], label);
                    assertEquals(0, correctedBitCount[0], label);
                }
            }
        }
    }

    @Test
    public void rejectsCorrectionIntoTheDeletedBits()
    {
        int[] decoded = new int[1];
        int[] correctedBitCount = new int[1];
        int rejected = 0;

        //Golay(24,12) codewords with 1 to 3 of the 6 deleted bits set: the 18-bit word received without them is
        //within 3 bits of that codeword, so Golay24 corrects it into the deleted bits.
        for(int data = 0; data < 4096; data++)
        {
            int deletedWeight = Integer.bitCount(data >> 6);

            if(deletedWeight < 1 || deletedWeight > 3)
            {
                continue;
            }

            int received = encode24(data) & 0x3FFFF;

            CorrectedBinaryMessage word = new CorrectedBinaryMessage(24);
            word.load(6, 18, received);
            Golay24.checkAndCorrect(word, 0);
            assertNotEquals(0, word.getInt(0, 5), "Golay24 sets a deleted bit, data " + data);

            int result = decode(received, decoded, correctedBitCount);
            String label = String.format("data %03X", data);
            assertEquals(2, result, label);
            assertEquals(received, decoded[0], label);
            assertEquals(0, correctedBitCount[0], label);
            rejected++;
        }

        //6 + 15 + 20 deleted bit patterns x 64 values of the other 6 data bits
        assertEquals(41 * 64, rejected);
    }
}
