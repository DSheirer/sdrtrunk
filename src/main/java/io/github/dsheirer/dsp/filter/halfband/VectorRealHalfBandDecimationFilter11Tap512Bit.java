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
 * This filter is optimized for an 11-tap half band filter running on a CPU that supports 512-bit/16-lane floating
 * point SIMD instructions.
 */
public class VectorRealHalfBandDecimationFilter11Tap512Bit implements IRealDecimationFilter
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_512;
    private static final int COEFFICIENTS_LENGTH = 11;

    private float[] mCoefficients = new float[16];
    private float[] mBuffer;
    private int mBufferOverlap;

    /**
     * Creates a half band filter with inherent decimation by two.
     *
     * @param coefficients of the half-band filter that is odd-length where all odd index coefficients are
     * zero valued except for the middle odd index coefficient which should be valued 0.5
     */
    public VectorRealHalfBandDecimationFilter11Tap512Bit(float[] coefficients)
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

        FloatVector filter = FloatVector.fromArray(VECTOR_SPECIES, mCoefficients, 0);

        FloatVector product;

        for(int bufferPointer = 0; bufferPointer < samples.length; bufferPointer += 2)
        {
            product = filter.mul(FloatVector.fromArray(VECTOR_SPECIES, mBuffer, bufferPointer));
            filtered[bufferPointer / 2] = product.reduceLanes(VectorOperators.ADD);
        }

        return filtered;
    }
}
