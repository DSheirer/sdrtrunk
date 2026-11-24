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

package io.github.dsheirer.vector.calibrate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Selecting an implementation from measured speedups, using measurements from calibration runs.
 */
public class CalibrationSelectionTest
{
    private static Map<Implementation,Double> speedups(Object... implementationAndSpeedup)
    {
        Map<Implementation,Double> speedups = new LinkedHashMap<>();
        speedups.put(Implementation.SCALAR, 1.0);

        for(int x = 0; x < implementationAndSpeedup.length; x += 2)
        {
            speedups.put((Implementation)implementationAndSpeedup[x], (Double)implementationAndSpeedup[x + 1]);
        }

        return speedups;
    }

    @Test
    void scalarWhenNoVectorBeatsTheMinimumSpeedup()
    {
        //Magnitude
        assertEquals(Implementation.SCALAR, Calibration.select(speedups(Implementation.VECTOR_SIMD_64, 0.81,
            Implementation.VECTOR_SIMD_128, 0.90, Implementation.VECTOR_SIMD_256, 1.00, Implementation.VECTOR_SIMD_512,
            1.03)));
    }

    @Test
    void narrowestOfEquivalentlyFastWidths()
    {
        //FIR filter: 256-bit is fastest and 512-bit is within the equivalent speedup, so 256-bit
        assertEquals(Implementation.VECTOR_SIMD_256, Calibration.select(speedups(Implementation.VECTOR_SIMD_64, 1.90,
            Implementation.VECTOR_SIMD_128, 3.14, Implementation.VECTOR_SIMD_256, 4.85, Implementation.VECTOR_SIMD_512,
            4.53)));

        //FIR filter, another run: 512-bit measured slightly faster than 256-bit, which still selects 256-bit
        assertEquals(Implementation.VECTOR_SIMD_256, Calibration.select(speedups(Implementation.VECTOR_SIMD_64, 1.89,
            Implementation.VECTOR_SIMD_128, 3.23, Implementation.VECTOR_SIMD_256, 4.48, Implementation.VECTOR_SIMD_512,
            4.54)));

        //Half-band 11 tap: 128-bit and 256-bit are equivalent, so 128-bit
        assertEquals(Implementation.VECTOR_SIMD_128, Calibration.select(speedups(Implementation.VECTOR_SIMD_64, 1.94,
            Implementation.VECTOR_SIMD_128, 3.06, Implementation.VECTOR_SIMD_256, 3.13, Implementation.VECTOR_SIMD_512,
            2.87)));
    }

    @Test
    void widerWhenClearlyFaster()
    {
        //FM demodulator
        assertEquals(Implementation.VECTOR_SIMD_512, Calibration.select(speedups(Implementation.VECTOR_SIMD_64, 0.78,
            Implementation.VECTOR_SIMD_128, 7.16, Implementation.VECTOR_SIMD_256, 12.87, Implementation.VECTOR_SIMD_512,
            31.13)));

        //DMR soft sync detector: 128-bit is fastest and nothing narrower is equivalent
        assertEquals(Implementation.VECTOR_SIMD_128, Calibration.select(speedups(Implementation.VECTOR_SIMD_64, 1.78,
            Implementation.VECTOR_SIMD_128, 2.24, Implementation.VECTOR_SIMD_256, 2.07, Implementation.VECTOR_SIMD_512,
            1.75)));
    }

    @Test
    void equivalentWidthMustAlsoBeatScalar()
    {
        //64-bit is within the equivalent speedup of 128-bit but doesn't beat scalar by the minimum speedup
        assertEquals(Implementation.VECTOR_SIMD_128, Calibration.select(speedups(Implementation.VECTOR_SIMD_64, 1.02,
            Implementation.VECTOR_SIMD_128, 1.06)));
    }

    @Test
    void preferredOnlyCandidate()
    {
        //Complex oscillator
        assertEquals(Implementation.VECTOR_SIMD_PREFERRED, Calibration.select(speedups(
            Implementation.VECTOR_SIMD_PREFERRED, 5.67)));
    }
}
