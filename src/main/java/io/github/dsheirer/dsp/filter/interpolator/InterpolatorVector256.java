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

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * Vector interpolator for 256 bit SIMD instructions.
 */
public class InterpolatorVector256 extends VectorInterpolator
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_256;

    @Override
    protected float vectorFilter(float[] samples, int offset, int index)
    {
        FloatVector tapsVector = FloatVector.fromArray(VECTOR_SPECIES, TAPS[index], 0);
        FloatVector sampleVector = FloatVector.fromArray(VECTOR_SPECIES, samples, offset);
        return sampleVector.mul(tapsVector).reduceLanes(VectorOperators.ADD);
    }
}
