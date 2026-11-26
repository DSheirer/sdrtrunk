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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that Golay(24,12,8) corrects up to three bit errors and reports four as uncorrectable.
 */
public class Golay24Test
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
    public void correctsThreeErrorsAndDetectsFour()
    {
        Random random = new Random(1);

        for(int trial = 0; trial < 16; trial++)
        {
            int data = random.nextInt(4096);
            int codeword = encode(data);

            for(int errors = 0; errors < (1 << 24); errors++)
            {
                int weight = Integer.bitCount(errors);

                if(weight > 4)
                {
                    continue;
                }

                CorrectedBinaryMessage message = new CorrectedBinaryMessage(24);
                message.load(0, 24, codeword ^ errors);
                int result = Golay24.checkAndCorrect(message, 0);
                String label = String.format("data %03X errors %06X", data, errors);

                if(weight == 4)
                {
                    assertEquals(2, result, label);
                }
                else
                {
                    assertTrue(result < 2, label);
                    assertEquals(data, message.getInt(0, 11), label);
                }
            }
        }
    }
}
