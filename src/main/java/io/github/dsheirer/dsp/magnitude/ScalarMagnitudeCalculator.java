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

package io.github.dsheirer.dsp.magnitude;

/**
 * Scalar implementation of magnitude
 */
public class ScalarMagnitudeCalculator implements IMagnitudeCalculator
{
    @Override
    public float[] calculate(float[] i, float[] q)
    {
        float[] magnitude = new float[i.length];

        for(int x = 0; x < i.length; x++)
        {
            magnitude[x] = ((i[x] * i[x]) + (q[x] * q[x]));
        }

        return magnitude;
    }
}
