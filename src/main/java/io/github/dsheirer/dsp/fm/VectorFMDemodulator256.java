/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.dsp.fm;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FM demodulator that uses JDK 17+ SIMD vector intrinsics
 */
public class VectorFMDemodulator256 extends VectorFMDemodulator
{
    private static final Logger mLog = LoggerFactory.getLogger(VectorFMDemodulator256.class);
    protected static final float ZERO = 0.0f;
    private static final int BUFFER_OVERLAP = 1;
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_256;

    //Initialize buffers to be non-null and resize once the first buffer arrives
    private float[] mIBuffer = new float[1];
    private float[] mQBuffer = new float[1];

    public VectorFMDemodulator256()
    {
    }

    @Override
    public float[] demodulate(float[] i, float[] q)
    {
        //Fallback to scalar implementation if the buffer size is not a multiple of the vector implementation.
        if(i.length % VECTOR_SPECIES.length() != 0)
        {
            return getScalarImplementation().demodulate(i,q);
        }

        if(mIBuffer.length != (i.length + BUFFER_OVERLAP))
        {
            mIBuffer = new float[i.length + BUFFER_OVERLAP];
            mQBuffer = new float[q.length + BUFFER_OVERLAP];
        }

        //Copy last sample to beginning of buffer arrays
        mIBuffer[0] = mIBuffer[mIBuffer.length - 1];
        mQBuffer[0] = mQBuffer[mQBuffer.length - 1];

        //Copy new samples to end of buffer arrays
        System.arraycopy(i, 0, mIBuffer, 1, i.length);
        System.arraycopy(q, 0, mQBuffer, 1, q.length);

        float[] demodulated = new float[i.length];

        FloatVector currentI, currentQ, previousI, previousQ, demod, demodI, demodQ;

        for(int bufferPointer = 0; bufferPointer < mIBuffer.length - 1; bufferPointer += VECTOR_SPECIES.length())
        {
            previousI = FloatVector.fromArray(VECTOR_SPECIES, mIBuffer, bufferPointer);
            previousQ = FloatVector.fromArray(VECTOR_SPECIES, mQBuffer, bufferPointer);
            currentI = FloatVector.fromArray(VECTOR_SPECIES, mIBuffer, bufferPointer + 1);
            currentQ = FloatVector.fromArray(VECTOR_SPECIES, mQBuffer, bufferPointer + 1);

            demodI = currentI.mul(previousI).sub(currentQ.mul(previousQ.neg()));
            demodQ = currentQ.mul(previousI).add(currentI.mul(previousQ.neg()));

            //Replace any zero-values in the I vector by adding Float.MIN_VALUE to side-step divide by zero errors
            demodI = demodI.add(Float.MIN_VALUE, demodQ.eq(ZERO));

            demod = demodQ.div(demodI).lanewise(VectorOperators.ATAN);
            demod.intoArray(demodulated, bufferPointer);
        }

        return demodulated;
    }
}
