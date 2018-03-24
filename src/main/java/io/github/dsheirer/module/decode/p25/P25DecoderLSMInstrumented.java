/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.dsp.psk.DQPSKGardnerDemodulatorInstrumented;
import io.github.dsheirer.dsp.psk.InterpolatingSampleBufferInstrumented;
import io.github.dsheirer.dsp.psk.SymbolDecisionData;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;
import io.github.dsheirer.sample.complex.reusable.ReusableComplexBuffer;

public class P25DecoderLSMInstrumented extends P25DecoderLSM
{
    private Listener<Double> mPLLPhaseErrorListener;
    private Listener<Double> mPLLFrequencyListener;
    private Listener<Double> mSamplesPerSymbolListener;
    private Listener<Complex> mComplexSymbolListener;
    private Listener<ReusableComplexBuffer> mFilteredSymbolListener;
    private Listener<SymbolDecisionData> mSymbolDecisionDataListener;

    /**
     * P25 Phase 1 - linear simulcast modulation (LSM) decoder.  Uses Differential QPSK decoding with a Costas PLL and
     * a gardner timing error detector.
     *
     * Instrumented decoder instance.
     *
     * @param aliasList
     */
    public P25DecoderLSMInstrumented(AliasList aliasList)
    {
        super(aliasList);
    }

    /**
     * Overrides the filter method so that we can capture the filtered samples for instrumentation
     */
    protected ReusableComplexBuffer filter(ReusableComplexBuffer reusableComplexBuffer)
    {
        ReusableComplexBuffer filtered = super.filter(reusableComplexBuffer);

        //Increment user count before we hand to another listener or return from this method
        filtered.incrementUserCount();

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

        InterpolatingSampleBufferInstrumented instrumentedBuffer = new InterpolatingSampleBufferInstrumented(getSamplesPerSymbol(), SYMBOL_TIMING_GAIN);
        mInterpolatingSampleBuffer = instrumentedBuffer;

        DQPSKGardnerDemodulatorInstrumented instrumented = new DQPSKGardnerDemodulatorInstrumented(mCostasLoop, instrumentedBuffer, getSampleRate());
        mQPSKDemodulator = instrumented;

        instrumented.setComplexSymbolListener(mComplexSymbolListener);
        instrumented.setPLLErrorListener(mPLLPhaseErrorListener);
        instrumented.setPLLFrequencyListener(mPLLFrequencyListener);
        instrumented.setSymbolDecisionDataListener(mSymbolDecisionDataListener);
        instrumented.setSamplesPerSymbolListener(mSamplesPerSymbolListener);
        instrumented.setSymbolListener(mMessageFramer);
    }

    public void setComplexSymbolListener(Listener<Complex> listener)
    {
        mComplexSymbolListener = listener;
        ((DQPSKGardnerDemodulatorInstrumented)mQPSKDemodulator).setComplexSymbolListener(listener);
    }

    public void setPLLPhaseErrorListener(Listener<Double> listener)
    {
        mPLLPhaseErrorListener = listener;
        ((DQPSKGardnerDemodulatorInstrumented)mQPSKDemodulator).setPLLErrorListener(listener);
    }

    public void setPLLFrequencyListener(Listener<Double> listener)
    {
        mPLLFrequencyListener = listener;
        ((DQPSKGardnerDemodulatorInstrumented)mQPSKDemodulator).setPLLFrequencyListener(listener);
    }

    public void setFilteredBufferListener(Listener<ReusableComplexBuffer> listener)
    {
        mFilteredSymbolListener = listener;
    }

    public void setSymbolDecisionDataListener(Listener<SymbolDecisionData> listener)
    {
        mSymbolDecisionDataListener = listener;
        ((DQPSKGardnerDemodulatorInstrumented)mQPSKDemodulator).setSymbolDecisionDataListener(listener);
    }

    public void setSamplesPerSymbolListener(Listener<Double> listener)
    {
        mSamplesPerSymbolListener = listener;
        ((DQPSKGardnerDemodulatorInstrumented)mQPSKDemodulator).setSamplesPerSymbolListener(listener);
    }
}