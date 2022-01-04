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
package io.github.dsheirer.dsp.filter.fir.complex;

import io.github.dsheirer.vector.VectorUtilities;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Finite Impulse Response (FIR) filter for filtering float sample arrays.
 * Note: this filter uses Project Panama SIMD instructions available in JDK 17+
 */
public class VectorComplexFIRFilter64Bit implements IComplexFilter
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_64;
    private static final VectorMask<Float> I_VECTOR_MASK = VectorUtilities.getIVectorMask(VECTOR_SPECIES);
    private static final VectorMask<Float> Q_VECTOR_MASK = VectorUtilities.getQVectorMask(VECTOR_SPECIES);
    private float[] mBuffer;
    private float[] mCoefficients;
    private int mBufferOverlap;

    /**
     * Float sample FIR filter base class.
     *
     * @param coefficients - filter coefficients in normal order.
     */
    public VectorComplexFIRFilter64Bit(float[] coefficients)
    {
        VectorUtilities.checkSpecies(VECTOR_SPECIES);

        //Size the coefficients array to a multiple of the vector species length, large enough to hold the taps;
        int arrayLength = VECTOR_SPECIES.length();

        while(arrayLength < (coefficients.length * 2))
        {
            arrayLength += VECTOR_SPECIES.length();
        }

        mCoefficients = new float[arrayLength];

        //Copy the coefficients with each tap duplicated for complex filtering
        ArrayUtils.reverse(coefficients);

        for(int x = 0; x < coefficients.length; x++)
        {
            mCoefficients[2 * x] = coefficients[x];
            mCoefficients[2 * x + 1] = coefficients[x];
        }

        //Set buffer overlap to larger of the length of the SIMD lanes minus 2 or double the coefficients length minus 2
        //to ensure we don't get an index out of bounds exception when loading samples from the buffer.
        mBufferOverlap = Math.max(arrayLength - 2, (coefficients.length * 2) - 2);

        //Set to non-null and we'll resize on the first buffer.
        mBuffer = new float[mCoefficients.length];
    }

    /**
     * Filters the sample array
     * @param samples to filter
     * @return filtered samples
     */
    public float[] filter(float[] samples)
    {
        int bufferLength = samples.length + mBufferOverlap;

        //Resize the data buffer if needed.  This shouldn't happen more than once since all buffers should be same size
        if(mBuffer.length != bufferLength)
        {
            float[] temp = new float[bufferLength];
            //Move residual samples from end of previous buffer to the beginning of the new temp buffer and reassign
            System.arraycopy(mBuffer, mBuffer.length - mBufferOverlap, temp, 0, mBufferOverlap);
            mBuffer = temp;
        }
        else
        {
            //Move residual samples from end of buffer to the beginning of the buffer
            System.arraycopy(mBuffer, samples.length, mBuffer, 0, mBufferOverlap);
        }

        //Copy new sample array to end of buffer
        System.arraycopy(samples, 0, mBuffer, mBufferOverlap, samples.length);

        float[] filtered = new float[samples.length];

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

            filtered[bufferPointer] = accumulator.reduceLanes(VectorOperators.ADD, I_VECTOR_MASK);
            filtered[bufferPointer + 1] = accumulator.reduceLanes(VectorOperators.ADD, Q_VECTOR_MASK);
        }

        return filtered;
    }
}