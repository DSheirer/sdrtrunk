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

package io.github.dsheirer.dsp.psk.dqpsk;

import io.github.dsheirer.dsp.filter.interpolator.Interpolator;
import io.github.dsheirer.dsp.filter.interpolator.InterpolatorFactory;
import io.github.dsheirer.sample.complex.ComplexSamples;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DQPSK demodulator that uses scalar calculations for demodulating the sample stream.
 */
public class DQPSKDemodulatorScalar extends DQPSKDemodulator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DQPSKDemodulatorScalar.class);

    private float[] mIBuffer = new float[20]; //Initial size 20 for array copy, but gets resized on first buffer
    private float[] mQBuffer = new float[20];
    private float mMu;
    private int mBufferOverlap;
    private int mInterpolationOffset;
    private final Interpolator mInterpolator = InterpolatorFactory.getInterpolator();

    /**
     * Constructor
     *
     * @param symbolRate symbols per second
     */
    public DQPSKDemodulatorScalar(int symbolRate)
    {
        super(symbolRate);
    }

    public void receive(ComplexSamples samples)
    {
        if(mSoftSymbolListener != null)
        {
            mSoftSymbolListener.setTimestamp(samples.timestamp());
        }

        int sampleLength = samples.i().length;
        int bufferOverlap = mBufferOverlap;

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
        System.arraycopy(samples.i(), 0, mIBuffer, bufferOverlap, sampleLength);
        System.arraycopy(samples.q(), 0, mQBuffer, bufferOverlap, sampleLength);
        //mIDecoded, mQDecoded and mPhase will be filled below during the decoding process.

        float[] decodedPhases = new float[samples.i().length];
        float iPrevious, qPreviousConjugate, iCurrent, qCurrent, differentialI, differentialQ;

        //Differential demodulation.
        for(int x = 0; x < sampleLength; x++)
        {
            iPrevious = mIBuffer[x];
            qPreviousConjugate = -mQBuffer[x]; //Complex Conjugate

            int offset = mInterpolationOffset + x;
            iCurrent = mInterpolator.filter(mIBuffer, offset, mMu);
            qCurrent = mInterpolator.filter(mQBuffer, offset, mMu);

            //Multiply current complex sample by the complex conjugate of previous complex sample.
            differentialI = (iPrevious * iCurrent) - (qPreviousConjugate * qCurrent);
            differentialQ = (iPrevious * qCurrent) + (iCurrent * qPreviousConjugate);

            decodedPhases[x] = (float)Math.atan2(differentialQ, differentialI);
        }

        //Dispatch decoded phases array to a symbol processor
        mSoftSymbolListener.receive(decodedPhases);
    }

    /**
     * Sets the sample rate
     *
     * @param sampleRate of the incoming sample stream
     */
    public void setSampleRate(double sampleRate)
    {
        super.setSampleRate(sampleRate);
        mMu = mSamplesPerSymbol % 1; //Fractional part of the samples per symbol rate
        mInterpolationOffset = (int) Math.floor(mSamplesPerSymbol) - 4; //Interpolate at the middle of 8x samples
        mBufferOverlap = (int) Math.floor(mSamplesPerSymbol) + 4;

        if(mSoftSymbolListener != null)
        {
            mSoftSymbolListener.setSamplesPerSymbol(mSamplesPerSymbol);
        }
    }
}
