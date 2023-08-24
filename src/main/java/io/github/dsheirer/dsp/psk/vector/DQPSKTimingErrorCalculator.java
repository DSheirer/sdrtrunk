/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.dsp.psk.vector;

import io.github.dsheirer.dsp.symbol.Dibit;

/**
 * Calculator for timing error for DQPSK constellations.
 */
public class DQPSKTimingErrorCalculator
{
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1 = (float)(Math.PI / 4.0);
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3 = 3.0f * IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1;
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_1 = -IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1;
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_3 = -IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3;

    /**
     * Calculates the timing error from the sample relative to the ideal sample point.
     * @param symbol decision
     * @param preceding sample, relative to the symbol decision sample.
     * @param sample interpolated, for the symbol decision.
     * @param following sample, relative to the symbol decision sample.
     * @return error signal in radians
     */
    public static float calculate(Dibit symbol, float preceding, float sample, float following)
    {
        float ideal = 0.0f;

        switch(symbol)
        {
            case D01_PLUS_3:
                ideal = IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3;
                break;
            case D00_PLUS_1:
                ideal = IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1;
                break;
            case D10_MINUS_1:
                ideal = IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_1;
                break;
            case D11_MINUS_3:
                ideal = IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_3;
                break;
        }

        float error = ideal - sample;
        return preceding < following ? error : -error;
    }
}
