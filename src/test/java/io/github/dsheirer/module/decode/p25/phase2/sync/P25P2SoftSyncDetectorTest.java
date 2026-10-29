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

package io.github.dsheirer.module.decode.p25.phase2.sync;

import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit tests for the P25 Phase 2 soft sync detector scalar and SIMD vector implementations.
 */
public class P25P2SoftSyncDetectorTest
{
    private static final float IDEAL_SCORE = (float)(20 * Math.pow(3.0 * Math.PI / 4.0, 2));

    private static P25P2SoftSyncDetector[] getDetectors()
    {
        return new P25P2SoftSyncDetector[]{new P25P2SoftSyncDetectorScalar(), new P25P2SoftSyncDetectorVector64(),
                new P25P2SoftSyncDetectorVector128(), new P25P2SoftSyncDetectorVector256(),
                new P25P2SoftSyncDetectorVector512()};
    }

    @Test
    public void detectsSyncPattern()
    {
        for(P25P2SoftSyncDetector detector : getDetectors())
        {
            float[] pattern = detector.getPatternSymbols();
            float score = 0;

            //Repeat the pattern so that the final symbol occurs at each of the circular buffer positions
            for(int repeat = 0; repeat < 3; repeat++)
            {
                for(float symbol : pattern)
                {
                    score = detector.process(symbol);
                }

                assertEquals(IDEAL_SCORE, score, 0.001, detector.getClass().getSimpleName());
            }
        }
    }

    @Test
    public void vectorImplementationsMatchScalar()
    {
        P25P2SoftSyncDetector[] detectors = getDetectors();
        Random random = new Random(1);

        for(int x = 0; x < 1000; x++)
        {
            float symbol = (float)((random.nextDouble() * 2.0 - 1.0) * Math.PI);
            float expected = detectors[0].process(symbol);

            for(int d = 1; d < detectors.length; d++)
            {
                assertEquals(expected, detectors[d].process(symbol), 0.001,
                        detectors[d].getClass().getSimpleName() + " at symbol " + x);
            }
        }
    }
}
