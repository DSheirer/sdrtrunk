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

package io.github.dsheirer.dsp.filter.interpolator;

/**
 * Linear interpolator for values in the range: -PI > 0 > PI with awareness of phase wrapping across the
 * (+/-) PI boundary.
 */
public class PhaseAwareLinearInterpolator
{
    public static final float TWO_PI = (float)(Math.PI * 2.0);

    /**
     * Calculates an interpolated value between x1 and x2 at a linear position mu between 0.0 and 1.0
     * @param x1 first value
     * @param x2 second value
     * @param mu offset between first and second values in range 0.0 to 1.0
     * @return interpolated value.
     */
    public static float calculate(float x1, float x2, float mu)
    {
        //Detect phase wrap at the +PI/-PI boundary ... ignore opposite side of the unit circle at 0 axis
        if(x1 * x2 < -Math.PI)
        {
            if(x1 > 0)
            {
                float value = x1 + ((TWO_PI + x2 - x1) * mu);

                if(value > Math.PI)
                {
                    return -TWO_PI + value;
                }
                else
                {
                    return value;
                }
            }
            else
            {
                float value = x1 + ((-TWO_PI + x2 - x1) * mu);

                if(value < -Math.PI)
                {
                    return TWO_PI + value;
                }
                else
                {
                    return value;
                }
            }
        }
        else
        {
            return x1 + ((x2 - x1) * mu);
        }
    }
}
