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

package io.github.dsheirer.dsp.filter.hilbert;

import io.github.dsheirer.sample.SampleUtils;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.vector.VectorUtilities;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * Implements a Hilbert transform using vector SIMD operations against a pre-defined 47-tap filter.
 * As described in Understanding Digital Signal Processing, Lyons, 3e, 2011, Section 13.37.2 (p 804-805)
 */
public class VectorHilbertTransformDefaultBits extends HilbertTransform
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_PREFERRED;

    /**
     * Filters the real samples array into complex samples at half the sample rate.
     * @param realSamples to filter
     * @param timestamp of the first sample
     * @return complex samples at half the sample rate
     */
    public ComplexSamples filter(float[] realSamples, long timestamp)
    {
        int bufferLength = realSamples.length / 2;

        //Resize the I and Q delay buffers to incoming buffer length plus overlap, if necessary.
        if(mIBuffer.length != (bufferLength + mIOverlap))
        {
            float[] iTemp = new float[bufferLength + mIOverlap];
            float[] qTemp = new float[bufferLength + mQOverlap];
            System.arraycopy(mIBuffer, 0, iTemp, 0, mIOverlap);
            System.arraycopy(mQBuffer, 0, qTemp, 0, mQOverlap);
            mIBuffer = iTemp;
            mQBuffer = qTemp;
        }

        ComplexSamples deinterleaved = SampleUtils.deinterleave(realSamples, timestamp);
        VectorUtilities.checkComplexArrayLength(deinterleaved.i(), deinterleaved.q(), VECTOR_SPECIES);

        System.arraycopy(deinterleaved.i(), 0, mIBuffer, mIOverlap, deinterleaved.i().length);
        System.arraycopy(deinterleaved.q(), 0, mQBuffer, mQOverlap, deinterleaved.q().length);

        float[] i = new float[bufferLength];
        float[] q = new float[bufferLength];

        FloatVector accumulator, filter, samples;

        for(int x = 0; x < bufferLength; x++)
        {
            accumulator = FloatVector.zero(VECTOR_SPECIES);

            for(int y = 0; y < mCoefficients.length; y += VECTOR_SPECIES.length())
            {
                filter = FloatVector.fromArray(VECTOR_SPECIES, mCoefficients, y);
                samples = FloatVector.fromArray(VECTOR_SPECIES, mQBuffer, x + y);
                accumulator = filter.fma(samples, accumulator);
            }

            i[x] = mIBuffer[x]; //Simple delay assignment
            q[x] = accumulator.reduceLanes(VectorOperators.ADD);
        }

        //Copy residual from end of delay buffers to beginning for next iteration
        System.arraycopy(mIBuffer, mIBuffer.length - mIOverlap, mIBuffer, 0, mIOverlap);
        System.arraycopy(mQBuffer, mQBuffer.length - mQOverlap, mQBuffer, 0, mQOverlap);

        return new ComplexSamples(i, q, timestamp);
    }
}
