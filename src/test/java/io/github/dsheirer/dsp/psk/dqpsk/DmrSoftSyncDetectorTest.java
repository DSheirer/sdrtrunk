/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.dsp.psk.dqpsk;

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.sync.DmrSoftSyncDetector;
import io.github.dsheirer.module.decode.dmr.sync.DmrSoftSyncDetectorScalar;
import io.github.dsheirer.module.decode.dmr.sync.DmrSoftSyncDetectorVector128;
import io.github.dsheirer.module.decode.dmr.sync.DmrSoftSyncDetectorVector256;
import io.github.dsheirer.module.decode.dmr.sync.DmrSoftSyncDetectorVector512;
import io.github.dsheirer.module.decode.dmr.sync.DmrSoftSyncDetectorVector64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * JUnit tests for the DMR Soft Sync Detector.
 */
public class DmrSoftSyncDetectorTest
{
    private static final float SYMBOL_VALUE = (float)(3.0 * Math.PI / 4.0);

    /**
     * Test: feed each sync pattern into each detector implementation and verify that each detector selected the
     * correct sync pattern.
     *
     * Success: all detector implementations correctly detect all sync patterns.
     */
    @Test
    void testAllSyncPatterns()
    {
        DmrSoftSyncDetector scalar = new DmrSoftSyncDetectorScalar();
        DmrSoftSyncDetector vector64 = new DmrSoftSyncDetectorVector64();
        DmrSoftSyncDetector vector128 = new DmrSoftSyncDetectorVector128();
        DmrSoftSyncDetector vector256 = new DmrSoftSyncDetectorVector256();
        DmrSoftSyncDetector vector512 = new DmrSoftSyncDetectorVector512();

        float symbol;

        for(DMRSyncPattern pattern: DMRSyncPattern.SYNC_PATTERNS)
        {
            for(Dibit dibit: pattern.toDibits())
            {
                symbol = toSymbol(dibit);
                scalar.process(symbol);
                vector64.process(symbol);
                vector128.process(symbol);
                vector256.process(symbol);
                vector512.process(symbol);
            }

            assertSame(scalar.getDetectedPattern(), pattern, "Scalar Detector Did Not Correctly Detect Pattern: " + pattern);
            assertSame(vector64.getDetectedPattern(), pattern, "Vector 64 Detector Did Not Correctly Detect Pattern: " + pattern);
            assertSame(vector128.getDetectedPattern(), pattern, "Vector 128 Detector Did Not Correctly Detect Pattern: " + pattern);
            assertSame(vector256.getDetectedPattern(), pattern, "Vector 256 Detector Did Not Correctly Detect Pattern: " + pattern);
            assertSame(vector512.getDetectedPattern(), pattern, "Vector 512 Detector Did Not Correctly Detect Pattern: " + pattern);
        }
    }

    /**
     * Converts the dibit to the optimal symbol value
     * @param dibit to convert
     * @return dibit's symbol value in radians.
     */
    private static float toSymbol(Dibit dibit)
    {
        return dibit == Dibit.D01_PLUS_3 ? SYMBOL_VALUE : -SYMBOL_VALUE;
    }
}
