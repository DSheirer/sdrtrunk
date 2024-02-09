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

package io.github.dsheirer.dsp.oscillator;

import java.util.Arrays;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * Real oscillator that uses SIMD vector intrinsics from JDK17+ Project Panama.
 */
public class VectorRealOscillator extends AbstractOscillator implements IRealOscillator
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_PREFERRED;
    private float[] mOffsets;

    public VectorRealOscillator(double frequency, double sampleRate)
    {
        super(frequency, sampleRate);

        mOffsets = new float[VECTOR_SPECIES.length()];
        for(int x = 0; x < mOffsets.length; x++)
        {
            mOffsets[x] = x + 1.0f;
        }
    }

    @Override
    public float[] generate(int sampleCount)
    {
        int length = sampleCount;

        //Increase the length until it aligns as a multiple of the vector species length
        while(length % VECTOR_SPECIES.length() != 0)
        {
            length++;
        }

        float[] samples = new float[length];

        for(int samplePointer = 0; samplePointer < sampleCount; samplePointer += VECTOR_SPECIES.length())
        {
            FloatVector generated = FloatVector.fromArray(VECTOR_SPECIES, mOffsets, 0);
            generated = generated.mul(mAnglePerSample);
            generated = generated.add(mCurrentPhase);
            mCurrentPhase = generated.lane(VECTOR_SPECIES.length() - 1);
            mCurrentPhase %= TWO_PI;
            generated = generated.lanewise(VectorOperators.SIN);
            generated.intoArray(samples, samplePointer);
        }

        //Truncate the array to the requested length, if we increased it for SIMD alignment.
        return Arrays.copyOf(samples, sampleCount);
    }
}
