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
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests Golay(24,12) correction of the data bits of words that are not at the start of a 24-bit message.
 */
public class Golay24OffsetTest
{
    private static int encode(int data)
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

    @Test
    public void correctsUpToThreeErrorsInEveryWordOfALongerMessage()
    {
        Random random = new Random(1);

        for(int trial = 0; trial < 2000; trial++)
        {
            int[] words = new int[3];
            CorrectedBinaryMessage message = new CorrectedBinaryMessage(72);

            for(int w = 0; w < 3; w++)
            {
                words[w] = encode(random.nextInt(4096));
                int received = words[w];

                for(int e = random.nextInt(4); e > 0; e--)
                {
                    received ^= 1 << random.nextInt(24);
                }

                message.load(w * 24, 24, received);
            }

            for(int w = 0; w < 3; w++)
            {
                Golay24.checkAndCorrect(message, w * 24);
            }

            for(int w = 0; w < 3; w++)
            {
                //The 12 data bits.  (The core correction can leave the overall parity bit wrong when it corrects other
                //bits, which no caller reads.)
                assertEquals(words[w] >> 12, message.getInt(w * 24, w * 24 + 11), "trial " + trial + " word " + w);
            }
        }
    }
}
