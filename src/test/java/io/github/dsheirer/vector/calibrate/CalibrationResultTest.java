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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Stored calibration result format.
 */
public class CalibrationResultTest
{
    @Test
    void formatParseRoundTrip()
    {
        Map<Implementation,Double> speedups = new LinkedHashMap<>();
        speedups.put(Implementation.SCALAR, 1.0);
        speedups.put(Implementation.VECTOR_SIMD_128, 3.2345);
        speedups.put(Implementation.VECTOR_SIMD_256, 3.1);
        CalibrationResult result = new CalibrationResult(Implementation.VECTOR_SIMD_128, speedups,
            List.of(Implementation.VECTOR_SIMD_512), 1234567890L);

        CalibrationResult parsed = CalibrationResult.parse(result.format()).orElseThrow();

        assertEquals(Implementation.VECTOR_SIMD_128, parsed.selected());
        assertEquals(List.of(Implementation.SCALAR, Implementation.VECTOR_SIMD_128, Implementation.VECTOR_SIMD_256),
            List.copyOf(parsed.speedups().keySet()));
        assertEquals(3.2345, parsed.getSelectedSpeedup(), 0.001);
        assertEquals(List.of(Implementation.VECTOR_SIMD_512), parsed.rejected());
        assertEquals(1234567890L, parsed.timestamp());
    }

    @Test
    void noRejected()
    {
        CalibrationResult result = new CalibrationResult(Implementation.SCALAR, Map.of(Implementation.SCALAR, 1.0),
            List.of(), 5L);
        CalibrationResult parsed = CalibrationResult.parse(result.format()).orElseThrow();
        assertTrue(parsed.rejected().isEmpty());
        assertEquals(1.0, parsed.getSelectedSpeedup());
    }

    @Test
    void malformedIsEmpty()
    {
        assertTrue(CalibrationResult.parse(null).isEmpty());
        assertTrue(CalibrationResult.parse("").isEmpty());
        assertTrue(CalibrationResult.parse("selected=NOT_AN_IMPLEMENTATION").isEmpty());
        assertTrue(CalibrationResult.parse("speedup=SCALAR:abc").isEmpty());
    }
}
