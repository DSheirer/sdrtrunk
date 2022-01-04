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
 * This filter is optimized for an 23-tap half band filter running on a CPU that supports 64-bit/2-lane floating
 * point SIMD instructions.
 */
public class VectorRealHalfBandDecimationFilter23Tap64Bit implements IRealDecimationFilter
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_64;
    private static final int COEFFICIENTS_LENGTH = 23;

    private float[] mCoefficients = new float[24];
    private float[] mBuffer;
    private int mBufferOverlap;

    /**
     * Creates a half band filter with inherent decimation by two.
     *
     * @param coefficients of the half-band filter that is odd-length where all odd index coefficients are
     * zero valued except for the middle odd index coefficient which should be valued 0.5
     */
    public VectorRealHalfBandDecimationFilter23Tap64Bit(float[] coefficients)
    {
        if(coefficients.length != COEFFICIENTS_LENGTH)
        {
            throw new IllegalArgumentException("Half-band filter coefficients must be " + COEFFICIENTS_LENGTH +
                    " taps.  You supplied a filter with " + coefficients.length + " taps.");
        }

        VectorUtilities.checkSpecies(VECTOR_SPECIES);

        //Copy the coefficients
        System.arraycopy(coefficients, 0, mCoefficients, 0, coefficients.length);

        //Set buffer overlap to larger of the length of the SIMD lanes (16) minus 1 or the coefficients length minus 1
        //to ensure we don't get an index out of bounds exception when loading samples from the buffer.
        mBufferOverlap = Math.max(VECTOR_SPECIES.length() -1, coefficients.length - 1);
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

        FloatVector filter1 = FloatVector.fromArray(VECTOR_SPECIES, mCoefficients, 0);
        FloatVector filter2 = FloatVector.fromArray(VECTOR_SPECIES, mCoefficients, 2);
        FloatVector filter3 = FloatVector.fromArray(VECTOR_SPECIES, mCoefficients, 4);
        FloatVector filter4 = FloatVector.fromArray(VECTOR_SPECIES, mCoefficients, 6);
        FloatVector filter5 = FloatVector.fromArray(VECTOR_SPECIES, mCoefficients, 8);
        FloatVector filter6 = FloatVector.fromArray(VECTOR_SPECIES, mCoefficients, 10);
        FloatVector filter7 = FloatVector.fromArray(VECTOR_SPECIES, mCoefficients, 12);
        FloatVector filter8 = FloatVector.fromArray(VECTOR_SPECIES, mCoefficients, 14);
        FloatVector filter9 = FloatVector.fromArray(VECTOR_SPECIES, mCoefficients, 16);
        FloatVector filter10 = FloatVector.fromArray(VECTOR_SPECIES, mCoefficients, 18);
        FloatVector filter11 = FloatVector.fromArray(VECTOR_SPECIES, mCoefficients, 20);
        FloatVector filter12 = FloatVector.fromArray(VECTOR_SPECIES, mCoefficients, 22);

        FloatVector product1, product2, product3, product4, product5, product6, product7, product8;
        FloatVector product9, product10, product11, product12;

        for(int bufferPointer = 0; bufferPointer < samples.length; bufferPointer += 2)
        {
            product1 = filter1.mul(FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer));
            product2 = filter2.mul(FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 2));
            product3 = filter3.mul(FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 4));
            product4 = filter4.mul(FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 6));
            product5 = filter5.mul(FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 8));
            product6 = filter6.mul(FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 10));
            product7 = filter7.mul(FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 12));
            product8 = filter8.mul(FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 14));
            product9 = filter9.mul(FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 16));
            product10 = filter10.mul(FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 18));
            product11 = filter11.mul(FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 20));
            product12 = filter12.mul(FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer + 22));

            filtered[bufferPointer / 2] = product1.add(product2).add(product3).add(product4).add(product5)
                    .add(product6).add(product7).add(product8).add(product9).add(product10).add(product11)
                    .add(product12).reduceLanes(VectorOperators.ADD);
        }

        return filtered;
    }
}
