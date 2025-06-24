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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Differential demodulator that uses scalar calculations for demodulating the sample stream.
 */
public class DifferentialDemodulatorScalar extends DifferentialDemodulator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DifferentialDemodulatorScalar.class);

    /**
     * Constructor
     *
     * @param symbolRate symbols per second
     */
    public DifferentialDemodulatorScalar(double sampleRate, int symbolRate)
    {
        super(sampleRate, symbolRate);
    }

    @Override
    public float[] demodulate(float[] i, float[] q)
    {
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
        //mIDecoded, mQDecoded and mPhase will be filled below during the decoding process.

        float[] decodedPhases = new float[i.length];
        float iPrevious, qPreviousConjugate, iCurrent, qCurrent, differentialI, differentialQ;

        //Differential demodulation.
        for(int x = 0; x < sampleLength; x++)
        {
            iPrevious = mIBuffer[x];
            qPreviousConjugate = -mQBuffer[x]; //Complex Conjugate

            int offset = mInterpolationOffset + x;
            iCurrent = mInterpolator.filter(mIBuffer, offset, mu);
            qCurrent = mInterpolator.filter(mQBuffer, offset, mu);

            //Multiply current complex sample by the complex conjugate of previous complex sample.
            differentialI = (iPrevious * iCurrent) - (qPreviousConjugate * qCurrent);
            differentialQ = (iPrevious * qCurrent) + (iCurrent * qPreviousConjugate);

            decodedPhases[x] = (float)Math.atan2(differentialQ, differentialI);
        }

        return decodedPhases;
    }
}
