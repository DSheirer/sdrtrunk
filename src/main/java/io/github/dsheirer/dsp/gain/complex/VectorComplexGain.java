/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.dsp.gain.complex;

import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.vector.VectorUtilities;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Vector implementation of complex gain that uses JDK 17+ SIMD instructions
 */
public class VectorComplexGain extends ComplexGain
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_PREFERRED;
    /**
     * Constructs an instance
     *
     * @param gain to apply to complex samples
     */
    public VectorComplexGain(float gain)
    {
        super(gain);
    }

    /**
     * Applies gain to the complex samples buffer
     * @param i samples to amplify
     * @param q samples to amplify
     * @return amplified samples.
     */
    @Override public ComplexSamples apply(float[] i, float[] q, long timestamp)
    {
        VectorUtilities.checkComplexArrayLength(i, q, VECTOR_SPECIES);

        for(int x = 0; x < i.length; x += VECTOR_SPECIES.length())
        {
            FloatVector.fromArray(VECTOR_SPECIES, i, x).mul(mGain).intoArray(i, x);
            FloatVector.fromArray(VECTOR_SPECIES, q, x).mul(mGain).intoArray(q, x);
        }

        return new ComplexSamples(i, q, timestamp);
    }
}
