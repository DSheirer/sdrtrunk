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

package io.github.dsheirer.dsp.psk.demod;

import java.util.Arrays;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Differential demodulator that uses Vector SIMD 512 calculations for demodulating the sample stream.
 */
public class DifferentialDemodulatorVector512 extends DifferentialDemodulator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DifferentialDemodulatorVector512.class);
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_512;

    /**
     * Constructor
     *
     * @param symbolRate symbols per second
     */
    public DifferentialDemodulatorVector512(double sampleRate, int symbolRate)
    {
        super(sampleRate, symbolRate);
    }

    @Override
    public float[] demodulate(float[] i, float[] q)
    {
        if(i.length % VECTOR_SPECIES.length() != 0)
        {
            return getScalarImplementation().demodulate(i,q);
        }

        int sampleLength = i.length;
        int bufferOverlap = mBufferOverlap;
        float mu = mMu;

        //Copy previous buffer residual samples to beginning of buffer.
        System.arraycopy(mIBuffer, mIBuffer.length - bufferOverlap, mIBuffer, 0, bufferOverlap);
        System.arraycopy(mQBuffer, mQBuffer.length - bufferOverlap, mQBuffer, 0, bufferOverlap);

        //Resize I/Q buffers if necessary
        int requiredBufferLength = sampleLength + bufferOverlap;
        if(mIBuffer.length != requiredBufferLength)
        {
            mIBuffer = Arrays.copyOf(mIBuffer, requiredBufferLength);
            mQBuffer = Arrays.copyOf(mQBuffer, requiredBufferLength);
        }

        //Append new samples to the residual samples from the previous buffer.
        System.arraycopy(i, 0, mIBuffer, bufferOverlap, sampleLength);
        System.arraycopy(q, 0, mQBuffer, bufferOverlap, sampleLength);

        float[] interpolatedI = new float[VECTOR_SPECIES.length()];
        float[] interpolatedQ = new float[VECTOR_SPECIES.length()];
        float[] decodedPhases = new float[sampleLength];
        FloatVector iPrevious, qPreviousConjugate, iCurrent, qCurrent, differentialI, differentialQ;

        //Differential demodulation.
        for(int x = 0; x < sampleLength; x += VECTOR_SPECIES.length())
        {
            iPrevious = FloatVector.fromArray(VECTOR_SPECIES, mIBuffer, x);
            qPreviousConjugate = FloatVector.fromArray(VECTOR_SPECIES, mQBuffer, x).neg(); //Complex Conjugate

            int offset = mInterpolationOffset + x;
            int index;
            for(int y = 0; y < VECTOR_SPECIES.length(); y++)
            {
                index = offset + y;
                interpolatedI[y] = mInterpolator.filter(mIBuffer, index, mu);
                interpolatedQ[y] = mInterpolator.filter(mQBuffer, index, mu);
            }
            iCurrent = FloatVector.fromArray(VECTOR_SPECIES, interpolatedI, 0);
            qCurrent = FloatVector.fromArray(VECTOR_SPECIES, interpolatedQ, 0);

            //Multiply current complex sample by the complex conjugate of previous complex sample.
            differentialI = iPrevious.mul(iCurrent).sub(qPreviousConjugate.mul(qCurrent));
            differentialQ = iPrevious.mul(qCurrent).add(iCurrent.mul(qPreviousConjugate));

            //Calculate phase angles using Arc Tangent and export to the decoded phases array.
            differentialQ.lanewise(VectorOperators.ATAN2, differentialI).intoArray(decodedPhases, x);
        }

        return decodedPhases;
    }
}
