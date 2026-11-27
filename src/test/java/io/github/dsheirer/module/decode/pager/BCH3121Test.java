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

package io.github.dsheirer.module.decode.pager;

import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class BCH3121Test
{
    @Test
    public void pocsagSyncAndIdleCodewordsHaveZeroSyndrome()
    {
        assertEquals(0, BCH3121.syndrome(0x7CD215D8 >>> 1));
        assertEquals(0, BCH3121.syndrome(0x7A89C197 >>> 1));
    }

    @Test
    public void correctsOneAndTwoBitErrors()
    {
        Random random = new Random(42);

        for(int trial = 0; trial < 2000; trial++)
        {
            int codeword = BCH3121.encode(random.nextInt(1 << 21));
            assertEquals(0, BCH3121.getErrorPattern(codeword));

            int a = random.nextInt(31);
            int b = (a + 1 + random.nextInt(30)) % 31;

            assertEquals(1 << a, BCH3121.getErrorPattern(codeword ^ (1 << a)));
            int pattern = (1 << a) | (1 << b);
            assertEquals(pattern, BCH3121.getErrorPattern(codeword ^ pattern));
        }
    }

    @Test
    public void threeBitErrorsAreNotCorrectedToTheOriginal()
    {
        int codeword = BCH3121.encode(0x12345);
        int errored = codeword ^ 0x7;
        int pattern = BCH3121.getErrorPattern(errored);

        if(pattern != BCH3121.UNCORRECTABLE)
        {
            assertNotEquals(codeword, errored ^ pattern);
        }
    }

    @Test
    public void reverse31()
    {
        assertEquals(1 << 30, BCH3121.reverse31(1));
        assertEquals(1, BCH3121.reverse31(1 << 30));
        assertEquals(0x12345678 & 0x7FFFFFFF, BCH3121.reverse31(BCH3121.reverse31(0x12345678)));
    }
}
