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
package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.dsp.psk.DQPSKDecisionDirectedDemodulatorInstrumented;
import io.github.dsheirer.dsp.psk.InterpolatingSampleBufferInstrumented;
import io.github.dsheirer.dsp.psk.SymbolDecisionData;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;
import io.github.dsheirer.sample.complex.ComplexSamples;

public class P25P1DecoderC4FMInstrumented extends P25P1DecoderC4FM
{
    private Listener<Double> mPLLPhaseErrorListener;
    private Listener<Double> mPLLFrequencyListener;
    private Listener<Double> mSamplesPerSymbolListener;
    private Listener<Complex> mComplexSymbolListener;
    private Listener<ComplexSamples> mFilteredSymbolListener;
    private Listener<SymbolDecisionData> mSymbolDecisionDataListener;

    /**
     * Instrumented version of the P25 C4FM decoder that supports registering listeners to provide access to data as
     * it is being processed by the decoder.
     */
    public P25P1DecoderC4FMInstrumented()
    {
    }

    /**
     * Primary method for processing incoming complex sample buffers
     * @param samples containing channelized complex samples
     */
    @Override
    public void receive(ComplexSamples samples)
    {
        mMessageFramer.setCurrentTime(samples.timestamp());

        float[] i = mIBasebandFilter.filter(samples.i());
        float[] q = mQBasebandFilter.filter(samples.q());

        if(mFilteredSymbolListener != null)
        {
            mFilteredSymbolListener.receive(new ComplexSamples(i, q, samples.timestamp()));
        }

        //Process the buffer for power meter measurements (before gain is applied)
        mPowerMonitor.process(i, q);

        ComplexSamples amplified = mAGC.process(i, q, samples.timestamp());
        mQPSKDemodulator.receive(amplified);
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

    public void setFilteredBufferListener(Listener<ComplexSamples> listener)
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