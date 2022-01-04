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
package io.github.dsheirer.dsp.filter.halfband;

import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.vector.VectorUtilities;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * Implements a half-band filter that produces one filtered output for every two input samples.
 *
 * This filter uses the Java Vector API for SIMD available in JDK 17+.
 *
 * This filter can be used for any tap length and uses SIMD 64-bit/2-lane instructions.
 */
public class VectorRealHalfBandDecimationFilter64Bit implements IRealDecimationFilter
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_64;
    private float[] mCoefficients;
    private float[] mBuffer;
    private int mBufferOverlap;

    /**
     * Creates a half band filter with inherent decimation by two.
     *
     * @param coefficients of the half-band filter that is odd-length where all odd index coefficients are
     * zero valued except for the middle odd index coefficient which should be valued 0.5
     */
    public VectorRealHalfBandDecimationFilter64Bit(float[] coefficients)
    {
        VectorUtilities.checkSpecies(VECTOR_SPECIES);

        //Size the coefficients array to a multiple of the vector species length, large enough to hold the taps;
        int arrayLength = VECTOR_SPECIES.length();

        while(arrayLength < coefficients.length)
        {
            arrayLength += VECTOR_SPECIES.length();
        }

        mCoefficients = new float[arrayLength];

        //Copy the coefficients
        System.arraycopy(coefficients, 0, mCoefficients, 0, coefficients.length);

        //Set buffer overlap to larger of the length of the SIMD lanes minus 1 or the coefficients length minus 1
        //to ensure we don't get an index out of bounds exception when loading samples from the buffer.
        mBufferOverlap = Math.max(arrayLength - 1, coefficients.length - 1);
    }

    public float[] decimateReal(float[] samples)
    {
        if(samples.length % 2 != 0)
        {
            throw new IllegalArgumentException("Samples array length must be an integer multiple of 2");
        }

        int bufferLength = samples.length + mBufferOverlap;

        if(mBuffer == null)
        {
            mBuffer = new float[bufferLength];
        }
        else if(mBuffer.length != bufferLength)
        {
            float[] temp = new float[bufferLength];
            //Move residual samples from end of old buffer to the beginning of the new temp buffer
            System.arraycopy(mBuffer, mBuffer.length - mBufferOverlap, temp, 0, mBufferOverlap);
            mBuffer = temp;
        }
        else
        {
            //Move residual samples from end of buffer to the beginning of the buffer
            System.arraycopy(mBuffer, samples.length, mBuffer, 0, mBufferOverlap);
        }

        //Copy new sample array into end of buffer
        System.arraycopy(samples, 0, mBuffer, mBufferOverlap, samples.length);

        float[] filtered = new float[samples.length / 2];

        FloatVector accumulator, buffer, filter;

        for(int bufferPointer = 0; bufferPointer < samples.length; bufferPointer += 2)
        {
            accumulator = FloatVector.zero(VECTOR_SPECIES);

            for(int coefficientPointer = 0; coefficientPointer < mCoefficients.length; coefficientPointer += VECTOR_SPECIES.length())
            {
                filter = FloatVector.fromArray(VECTOR_SPECIES, mCoefficients, coefficientPointer);
                buffer = FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + coefficientPointer);
                accumulator = filter.fma(buffer, accumulator);
            }

            filtered[bufferPointer / 2] = accumulator.reduceLanes(VectorOperators.ADD);
        }

        return filtered;
    }
}
