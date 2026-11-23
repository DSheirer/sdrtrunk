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
package io.github.dsheirer.buffer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AveragingBuffersTest
{
    @Test
    void floatAverageReplacesNonFiniteSamplesWithZero()
    {
        FloatAveragingBuffer buffer = new FloatAveragingBuffer(2);
        assertEquals(2, buffer.get(4));
        assertEquals(5, buffer.get(6));
        assertEquals(3, buffer.get(Float.NaN));
        assertEquals(0, buffer.get(Float.POSITIVE_INFINITY));
        assertEquals(4, buffer.get(8));
    }

    @Test
    void floatResetRestartsRapidFill()
    {
        FloatAveragingBuffer buffer = new FloatAveragingBuffer(3, 2);
        assertEquals(2, buffer.get(3));
        assertEquals(5, buffer.get(6));
        assertEquals(7, buffer.get(9));

        buffer.reset();
        assertEquals(0, buffer.getAverage());
        assertEquals(4, buffer.get(6));
    }

    @Test
    void dcCorrectionAppliesGainToResidualAndStopsAfterConverging()
    {
        DcCorrectionManager manager = new DcCorrectionManager();
        manager.adjust(1.0f);
        assertEquals(0.05f, manager.getAverageDc(), 0.000001f);
        manager.adjust(1.0f);
        assertEquals(0.0975f, manager.getAverageDc(), 0.000001f);

        for(int x = 0; x < 5; x++)
        {
            assertTrue(manager.shouldCalculateDc());
            manager.adjust(manager.getAverageDc());
        }

        assertFalse(manager.shouldCalculateDc());
        assertFalse(manager.shouldCalculateDc());
    }
}
