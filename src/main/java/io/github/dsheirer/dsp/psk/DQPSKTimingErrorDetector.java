/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.dsp.psk;

import io.github.dsheirer.sample.complex.Complex;

public class DQPSKTimingErrorDetector
{
    private static final float PI_4_ROTATION = (float)(Math.PI / 4.0);

    /**
     * Custom early/late symbol timing error detector designed to optimize the sample timing point such that the
     * current symbol's inphase and quadrature approach the optimal 45 degree (.707) sampling instant.
     */
    public DQPSKTimingErrorDetector()
    {
    }

    /**
     * Calculates a symbol sampling time error by comparing the current complex sample against a normal QPSK or
     * Pi/4 rotated four symbol constellation.  The error signal (advance/retard) is determined by the slope
     * of each of the inphase and quadrature legs as detected by comparing an immediately preceding sample against
     * the current sample and then calculating the error distance of the sampled symbol from the ideal Pi/4 sampling
     * location.
     *
     * Note: both the preceding and the current symbols MUST be normalized (ie symbol.normalize()) to the unit circle
     * prior to invoking this method.
     *
     * @param precedingSymbol
     * @param currentSymbol
     * @return timing error signal used to advance (positive) or retard (negative) the sampling point
     */
    public float getError(Complex precedingSymbol, Complex currentSymbol)
    {
        return getError(precedingSymbol.inphase(), currentSymbol.inphase()) +
               getError(precedingSymbol.quadrature(), currentSymbol.quadrature());
    }

    /**
     * Calculates timing error of current value relative to an ideal 45 degree or Pi/4 radians (.707) value.
     *
     * @param early sample that closely precedes the current sampling instant - used to determine slope
     * @param current sampling value
     * @return error as the distance of the current value from an ideal value and the sign (advance/retard)
     * appropriate for the detected slope
     */
    private float getError(float early, float current)
    {
        if(early < current) //Ascending slope
        {
            if(current > 0.0)
            {
                //Positive quadrature hemisphere, ascending slope
                return PI_4_ROTATION - current;
            }
            else
            {
                //Negative quadrature hemisphere, ascending slope
                return -PI_4_ROTATION - current;
            }
        }
        else //Descending slope
        {
            if(current > 0.0)
            {
                //Positive quadrature hemisphere, descending slope
                return current - PI_4_ROTATION;
            }
            else
            {
                //Negative quadrature hemisphere, descending slope
                return current + PI_4_ROTATION;
            }
        }
    }
}
