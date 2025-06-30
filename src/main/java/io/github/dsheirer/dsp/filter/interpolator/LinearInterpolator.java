/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
 * Linear interpolator.
 */
public class LinearInterpolator
{
    /**
     * Calculates an interpolated value between x1 and x2 at a linear position mu between 0.0 and 1.0
     * @param x1 first value
     * @param x2 second value
     * @param mu offset between first and second values in range 0.0 to 1.0
     * @return interpolated value.
     */
    public static float calculate(float x1, float x2, float mu)
    {
        if(mu < 0)
        {
            return x1;
        }
        else if(mu > 1)
        {
            return x2;
        }

        return x1 + ((x2 - x1) * mu);
    }

    public static float calculate(float x1, float x2, double mu)
    {
        return calculate(x1, x2, (float)mu);
    }
}
