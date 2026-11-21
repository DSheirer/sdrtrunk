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

package io.github.dsheirer.module.decode.smartnet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Type II voice high-pass removes the low-speed data band and passes voice.
 */
public class LowSpeedDataFilterTest
{
    private static double responseDb(float[] h, double frequency)
    {
        double re = 0, im = 0;

        for(int n = 0; n < h.length; n++)
        {
            re += h[n] * Math.cos(2 * Math.PI * frequency * n / 8000.0);
            im -= h[n] * Math.sin(2 * Math.PI * frequency * n / 8000.0);
        }

        return 20 * Math.log10(Math.hypot(re, im) + 1e-12);
    }

    @Test
    void testResponse()
    {
        float[] h = LowSpeedDataFilter.getCoefficients();
        assertNotNull(h, "filter designed");
        assertNotNull(LowSpeedDataFilter.create());

        for(double f : new double[]{50, 75, 112, 150, 225, 250})
        {
            assertTrue(responseDb(h, f) < -45, f + " Hz: " + responseDb(h, f));
        }

        for(double f : new double[]{400, 1000, 2000, 3000})
        {
            assertTrue(Math.abs(responseDb(h, f)) < 0.5, f + " Hz: " + responseDb(h, f));
        }
    }
}
