/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.dsp.psk.DQPSKGardnerDemodulatorInstrumented;
import io.github.dsheirer.dsp.psk.InterpolatingSampleBufferInstrumented;
import io.github.dsheirer.dsp.psk.SymbolDecisionData;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.complex.Complex;

public class P25P2DecoderHDQPSKInstrumented extends P25P2DecoderHDQPSK
{
    private Listener<Double> mPLLPhaseErrorListener;
    private Listener<Double> mPLLFrequencyListener;
    private Listener<Double> mSamplesPerSymbolListener;
    private Listener<Complex> mComplexSymbolListener;
    private Listener<ReusableComplexBuffer> mFilteredSymbolListener;
    private Listener<SymbolDecisionData> mSymbolDecisionDataListener;

    /**
     * Instrumented version of the P25 C4FM decoder that supports registering listeners to provide access to data as
     * it is being processed by the decoder.
     */
    public P25P2DecoderHDQPSKInstrumented(DecodeConfigP25Phase2 decodeConfigP25Phase2)
    {
        super((decodeConfigP25Phase2));
    }

    /**
     * Demodulator
     */
    public DQPSKGardnerDemodulatorInstrumented getDemodulator()
    {
        return (DQPSKGardnerDemodulatorInstrumented)mQPSKDemodulator;
    }

    /**
     * Overrides the filter method so that we can capture the filtered samples for instrumentation
     */
    protected ReusableComplexBuffer filter(ReusableComplexBuffer reusableComplexBuffer)
    {
        ReusableComplexBuffer filtered = super.filter(reusableComplexBuffer);

        if(mFilteredSymbolListener != null)
        {
            filtered.incrementUserCount();
            mFilteredSymbolListener.receive(filtered);
        }

        return filtered;
    }

    /**
     * Instrumented interpolating sample buffer
     */
    public InterpolatingSampleBufferInstrumented getSampleBuffer()
    {
        return (InterpolatingSampleBufferInstrumented)mInterpolatingSampleBuffer;
    }

    /**
     * Overrides this method so we can correctly configure for instrumented operations
     */
    public void setSampleRate(double sampleRate)
    {
        super.setSampleRate(sampleRate);

        InterpolatingSampleBufferInstrumented instrumentedBuffer =
            new InterpolatingSampleBufferInstrumented(getSamplesPerSymbol(), SYMBOL_TIMING_GAIN);
        mInterpolatingSampleBuffer = instrumentedBuffer;

        DQPSKGardnerDemodulatorInstrumented instrumented = new DQPSKGardnerDemodulatorInstrumented(mCostasLoop, instrumentedBuffer, getSampleRate());
        mQPSKDemodulator = instrumented;

        instrumented.setComplexSymbolListener(mComplexSymbolListener);
        instrumented.setPLLErrorListener(mPLLPhaseErrorListener);
        instrumented.setPLLFrequencyListener(mPLLFrequencyListener);
        instrumented.setSymbolDecisionDataListener(mSymbolDecisionDataListener);
        instrumented.setSamplesPerSymbolListener(mSamplesPerSymbolListener);
        instrumented.setSymbolListener(getDibitBroadcaster());
        getDibitBroadcaster().addListener(mMessageFramer);
    }

    public void setComplexSymbolListener(Listener<Complex> listener)
    {
        mComplexSymbolListener = listener;
        getDemodulator().setComplexSymbolListener(listener);
    }

    public void setPLLPhaseErrorListener(Listener<Double> listener)
    {
        mPLLPhaseErrorListener = listener;
        getDemodulator().setPLLErrorListener(listener);
    }

    public void setPLLFrequencyListener(Listener<Double> listener)
    {
        mPLLFrequencyListener = listener;
        getDemodulator().setPLLFrequencyListener(listener);
    }

    public void setFilteredBufferListener(Listener<ReusableComplexBuffer> listener)
    {
        mFilteredSymbolListener = listener;
    }

    public void setSymbolDecisionDataListener(Listener<SymbolDecisionData> listener)
    {
        mSymbolDecisionDataListener = listener;
        getDemodulator().setSymbolDecisionDataListener(listener);
    }

    public void setSamplesPerSymbolListener(Listener<Double> listener)
    {
        mSamplesPerSymbolListener = listener;
        getDemodulator().setSamplesPerSymbolListener(listener);
    }
}