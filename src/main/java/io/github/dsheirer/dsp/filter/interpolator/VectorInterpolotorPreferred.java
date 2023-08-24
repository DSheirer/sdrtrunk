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

package io.github.dsheirer.dsp.filter.interpolator;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * Vector interpolator for the preferred SIMD instructions.
 */
public class VectorInterpolotorPreferred extends VectorInterpolator
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_PREFERRED;

    @Override
    protected float vectorFilter(float[] samples, int offset, int index)
    {
        FloatVector accumulator = FloatVector.zero(VECTOR_SPECIES);
        FloatVector tapsVector, sampleVector;

        for(int x = 0; x < 8; x += VECTOR_SPECIES.length())
        {
            tapsVector = FloatVector.fromArray(VECTOR_SPECIES, TAPS[index], x);
            sampleVector = FloatVector.fromArray(VECTOR_SPECIES, samples, offset + x);
            accumulator = tapsVector.fma(sampleVector, accumulator);
        }

        return accumulator.reduceLanes(VectorOperators.ADD);
    }

    public static void main(String[] args)
    {
        Interpolator interpolator = InterpolatorFactory.getInterpolator();
        float[] samples = new float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f};
        for(float mu = 0.0f; mu <= 1.0f; mu += 0.05f)
        {
            float interpolated = interpolator.filter(samples, 0, mu);
            System.out.println("Mu:" + mu + " Interpolated:" + interpolated);
        }
    }
}
