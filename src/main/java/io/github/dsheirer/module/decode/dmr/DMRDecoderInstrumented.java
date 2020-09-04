/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.dsp.psk.DQPSKDecisionDirectedDemodulatorInstrumented;
import io.github.dsheirer.dsp.psk.InterpolatingSampleBufferInstrumented;
import io.github.dsheirer.dsp.psk.SymbolDecisionData;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.complex.Complex;

/**
 * Instrumented version of DMR decoder
 */
public class DMRDecoderInstrumented extends DMRDecoder
{
    private Listener<Double> mPLLPhaseErrorListener;
    private Listener<Double> mPLLFrequencyListener;
    private Listener<Double> mSamplesPerSymbolListener;
    private Listener<Complex> mComplexSymbolListener;
    private Listener<ReusableComplexBuffer> mFilteredSymbolListener;
    private Listener<SymbolDecisionData> mSymbolDecisionDataListener;

    /**
     * Instrumented version of the DMR decoder that supports registering listeners to provide access to data as
     * it is being processed by the decoder.
     */
    public DMRDecoderInstrumented(DecodeConfigDMR config)
    {
        super(config);
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
     * Overrides this method so we can correctly configure for instrumented operations
     */
    public void setSampleRate(double sampleRate)
    {
        super.setSampleRate(sampleRate);

        InterpolatingSampleBufferInstrumented instrumentedBuffer =
            new InterpolatingSampleBufferInstrumented(getSamplesPerSymbol(), SAMPLE_COUNTER_GAIN);
        mInterpolatingSampleBuffer = instrumentedBuffer;

        DQPSKDecisionDirectedDemodulatorInstrumented instrumented = new DQPSKDecisionDirectedDemodulatorInstrumented(mCostasLoop, instrumentedBuffer, getSampleRate());
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
        ((DQPSKDecisionDirectedDemodulatorInstrumented)mQPSKDemodulator).setComplexSymbolListener(listener);
    }

    public void setPLLPhaseErrorListener(Listener<Double> listener)
    {
        mPLLPhaseErrorListener = listener;
        ((DQPSKDecisionDirectedDemodulatorInstrumented)mQPSKDemodulator).setPLLErrorListener(listener);
    }

    public void setPLLFrequencyListener(Listener<Double> listener)
    {
        mPLLFrequencyListener = listener;
        ((DQPSKDecisionDirectedDemodulatorInstrumented)mQPSKDemodulator).setPLLFrequencyListener(listener);
    }

    public void setFilteredBufferListener(Listener<ReusableComplexBuffer> listener)
    {
        mFilteredSymbolListener = listener;
    }

    public void setSymbolDecisionDataListener(Listener<SymbolDecisionData> listener)
    {
        mSymbolDecisionDataListener = listener;
        ((DQPSKDecisionDirectedDemodulatorInstrumented)mQPSKDemodulator).setSymbolDecisionDataListener(listener);
    }

    public void setSamplesPerSymbolListener(Listener<Double> listener)
    {
        mSamplesPerSymbolListener = listener;
        ((DQPSKDecisionDirectedDemodulatorInstrumented)mQPSKDemodulator).setSamplesPerSymbolListener(listener);
    }
}