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

import io.github.dsheirer.vector.VectorUtilities;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Vector implementation of magnitude calculation.
 */
public class VectorMagnitudeCalculator64 implements IMagnitudeCalculator
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_64;

    @Override
    public float[] calculate(float[] i, float[] q)
    {
        VectorUtilities.checkComplexArrayLength(i, q, VECTOR_SPECIES);
        float[] magnitude = new float[i.length];

        FloatVector iVector, qVector, result;
        for(int x = 0; x < i.length; x += VECTOR_SPECIES.length())
        {
            iVector = FloatVector.fromArray(VECTOR_SPECIES, i, x);
            qVector = FloatVector.fromArray(VECTOR_SPECIES, q, x);
            result = iVector.mul(iVector).add(qVector.mul(qVector));
            result.intoArray(magnitude, x);
        }

        return magnitude;
    }
}
