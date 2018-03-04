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
import io.github.dsheirer.dsp.psk.DQPSKDemodulatorInstrumented;
import io.github.dsheirer.dsp.psk.InterpolatingSampleBufferInstrumented;
import io.github.dsheirer.dsp.psk.SymbolDecisionData;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;
import io.github.dsheirer.sample.complex.ComplexBuffer;

public class P25_C4FMDecoder2Instrumented extends P25_C4FMDecoder2
{
    private Listener<Double> mPLLPhaseErrorListener;
    private Listener<Double> mPLLFrequencyListener;
    private Listener<Double> mSamplesPerSymbolListener;
    private Listener<Complex> mSymbolListener;
    private Listener<ComplexBuffer> mFilteredSymbolListener;
    private Listener<SymbolDecisionData> mSymbolDecisionDataListener;

    public P25_C4FMDecoder2Instrumented(AliasList aliasList)
    {
        super(aliasList);
    }

    /**
     * Overrides the filter method so that we can capture the filtered samples for instrumentation
     */
    protected ComplexBuffer filter(ComplexBuffer complexBuffer)
    {
        ComplexBuffer filtered = super.filter(complexBuffer);

        if(mFilteredSymbolListener != null)
        {
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
        InterpolatingSampleBufferInstrumented instrumentedBuffer = new InterpolatingSampleBufferInstrumented((float)(sampleRate / SYMBOL_RATE));
        mInterpolatingSampleBuffer = instrumentedBuffer;

        DQPSKDemodulatorInstrumented instrumented = new DQPSKDemodulatorInstrumented(mCostasLoop, instrumentedBuffer, mSampleRate);
        mQPSKDemodulator = instrumented;

        instrumented.setSymbolListener(mSymbolListener);
        instrumented.setPLLErrorListener(mPLLPhaseErrorListener);
        instrumented.setPLLFrequencyListener(mPLLFrequencyListener);
        instrumented.setSymbolDecisionDataListener(mSymbolDecisionDataListener);
        instrumented.setSamplesPerSymbolListener(mSamplesPerSymbolListener);
        instrumented.setDibitListener(mMessageFramer);
    }

    public void setSymbolListener(Listener<Complex> listener)
    {
        mSymbolListener = listener;
        ((DQPSKDemodulatorInstrumented)mQPSKDemodulator).setSymbolListener(listener);
    }

    public void setPLLPhaseErrorListener(Listener<Double> listener)
    {
        mPLLPhaseErrorListener = listener;
        ((DQPSKDemodulatorInstrumented)mQPSKDemodulator).setPLLErrorListener(listener);
    }

    public void setPLLFrequencyListener(Listener<Double> listener)
    {
        mPLLFrequencyListener = listener;
        ((DQPSKDemodulatorInstrumented)mQPSKDemodulator).setPLLFrequencyListener(listener);
    }

    public void setFilteredBufferListener(Listener<ComplexBuffer> listener)
    {
        mFilteredSymbolListener = listener;
    }

    public void setSymbolDecisionDataListener(Listener<SymbolDecisionData> listener)
    {
        mSymbolDecisionDataListener = listener;
        ((DQPSKDemodulatorInstrumented)mQPSKDemodulator).setSymbolDecisionDataListener(listener);
    }

    public void setSamplesPerSymbolListener(Listener<Double> listener)
    {
        mSamplesPerSymbolListener = listener;
        ((DQPSKDemodulatorInstrumented)mQPSKDemodulator).setSamplesPerSymbolListener(listener);
    }
}