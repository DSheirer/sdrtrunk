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

package io.github.dsheirer.module.decode.dmr.message.data;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.DMRBurstTestUtility;
import io.github.dsheirer.module.decode.dmr.message.type.DataType;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests slot type Golay(20,8) validity: up to three bit errors are corrected and four or more are not reported as a
 * valid slot type.
 */
public class SlotTypeTest
{
    //Slot type bits in the 288-bit burst: 122-131 and 180-189 (see DMRBurstTestUtility.createBurst)
    private static final int[] INDEXES = new int[]{122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 180, 181, 182,
        183, 184, 185, 186, 187, 188, 189};

    private static CorrectedBinaryMessage burst(int received)
    {
        CorrectedBinaryMessage burst = new CorrectedBinaryMessage(288);

        for(int x = 0; x < 20; x++)
        {
            if((received & (1 << (19 - x))) != 0)
            {
                burst.set(INDEXES[x]);
            }
        }

        return burst;
    }

    private static int codeword(int colorCode, DataType dataType)
    {
        boolean[] bits = DMRBurstTestUtility.createSlotType(colorCode, dataType);
        int word = 0;

        for(boolean bit : bits)
        {
            word = (word << 1) | (bit ? 1 : 0);
        }

        return word;
    }

    @Test
    public void correctsThreeErrorsAndRejectsFour()
    {
        int[][] slotTypes = {{0, 3}, {1, 0}, {5, 9}, {7, 3}, {10, 6}, {15, 15}};

        for(int[] slotType : slotTypes)
        {
            int colorCode = slotType[0];
            DataType dataType = DataType.fromValue(slotType[1]);
            int codeword = codeword(colorCode, dataType);

            for(int errors = 0; errors < (1 << 20); errors++)
            {
                int weight = Integer.bitCount(errors);

                if(weight > 4)
                {
                    continue;
                }

                SlotType decoded = SlotType.getSlotType(burst(codeword ^ errors));
                String label = String.format("CC %d %s errors %05X", colorCode, dataType, errors);

                if(weight == 4)
                {
                    assertFalse(decoded.isValid(), label);
                }
                else
                {
                    assertTrue(decoded.isValid(), label);
                    assertEquals(colorCode, decoded.getColorCode(), label);
                    assertEquals(dataType, decoded.getDataType(), label);
                }
            }
        }
    }

    @Test
    public void randomBitsAreValidOnlyWithinThreeBitsOfACodeword()
    {
        Random random = new Random(1);
        int valid = 0;
        int trials = 100000;

        for(int trial = 0; trial < trials; trial++)
        {
            int received = random.nextInt(1 << 20);
            SlotType decoded = SlotType.getSlotType(burst(received));

            if(decoded.isValid())
            {
                valid++;
                int distance = Integer.bitCount(received ^ codeword(decoded.getColorCode(), decoded.getDataType()));
                assertTrue(distance <= 3, String.format("received %05X distance %d", received, distance));
            }
        }

        //256 codewords x 1351 words within 3 bits of each, out of 2^20: 33%
        assertTrue(valid < trials * 0.35, "valid " + valid + " of " + trials);
    }
}
