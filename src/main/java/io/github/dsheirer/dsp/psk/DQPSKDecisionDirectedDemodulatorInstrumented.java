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
package io.github.dsheirer.dsp.psk;

import io.github.dsheirer.dsp.psk.pll.CostasLoop;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.complex.Complex;

public class DQPSKDecisionDirectedDemodulatorInstrumented extends DQPSKDecisionDirectedDemodulator
{
    private Listener<SymbolDecisionData> mSymbolDecisionDataListener;
    private Listener<Double> mSamplesPerSymbolListener;
    private Listener<Complex> mComplexSymbolListener;
    private Listener<Double> mPLLErrorListener;
    private Listener<Double> mPLLFrequencyListener;
    private Listener<ReusableComplexBuffer> mFilteredGainAppliedComplexBufferListener;
    private double mSampleRate;

    /**
     * Decoder for Differential Quaternary Phase Shift Keying (DQPSK).  This decoder uses both a Costas Loop (PLL) and
     * a custom DQPSK symbol timing error detector to automatically align to the incoming carrier frequency and to
     * adjust for any changes in symbol timing.
     *
     * This detector is optimized for constant amplitude DQPSK symbols like C4FM.
     *
     * @param phaseLockedLoop for tracking carrier frequency error
     * @param interpolatingSampleBuffer
     * @param sampleRate of the incoming complex sample stream
     */
    public DQPSKDecisionDirectedDemodulatorInstrumented(IPhaseLockedLoop phaseLockedLoop,
                                                        InterpolatingSampleBufferInstrumented interpolatingSampleBuffer,
                                                        double sampleRate)
    {
        super(phaseLockedLoop, interpolatingSampleBuffer);
        mSampleRate = sampleRate;
    }

    @Override
    public void receive(ReusableComplexBuffer reusableComplexBuffer)
    {
        if(mFilteredGainAppliedComplexBufferListener != null)
        {
            reusableComplexBuffer.incrementUserCount();
            mFilteredGainAppliedComplexBufferListener.receive(reusableComplexBuffer);
        }

        super.receive(reusableComplexBuffer);
    }

    /**
     * Overrides the parent class symbol calculation to capture eye diagram data
     */
    protected void calculateSymbol()
    {
        //Eye diagram listener
        if(mSymbolDecisionDataListener != null)
        {
            mSymbolDecisionDataListener.receive(
                ((InterpolatingSampleBufferInstrumented)getInterpolatingSampleBuffer()).getSymbolDecisionData());
        }

        super.calculateSymbol();

        if(mSamplesPerSymbolListener != null)
        {
            mSamplesPerSymbolListener.receive((double)getInterpolatingSampleBuffer().getSamplingPoint());
        }

        //Send to an external constellation symbol listener when registered
        if(mComplexSymbolListener != null)
        {
            mComplexSymbolListener.receive(mCurrentSymbol);
        }

        if(mPLLErrorListener != null)
        {
            mPLLErrorListener.receive((double)mSymbolEvaluator.getPhaseError());
        }

        if(mPLLFrequencyListener != null)
        {
            double loopFrequency = ((CostasLoop)getPLL()).getLoopFrequency();

            loopFrequency *= mSampleRate / (2.0 * Math.PI);

            mPLLFrequencyListener.receive(loopFrequency);
        }
    }

    /**
     * Registers a listener to receive symbol decision data to produce an eye diagram.
     */
    public void setSymbolDecisionDataListener(Listener<SymbolDecisionData> listener)
    {
        mSymbolDecisionDataListener = listener;
    }

    /**
     * Registers the listener to receive PLL error values
     */
    public void setSamplesPerSymbolListener(Listener<Double> listener)
    {
        mSamplesPerSymbolListener = listener;
    }

    /**
     * Registers the listener to receive decoded QPSK symbols
     */
    public void setComplexSymbolListener(Listener<Complex> listener)
    {
        mComplexSymbolListener = listener;
    }

    /**
     * Registers the listener to receive PLL error values
     */
    public void setPLLErrorListener(Listener<Double> listener)
    {
        mPLLErrorListener = listener;
    }

    /**
     * Registers the listener to receive PLL error values
     */
    public void setPLLFrequencyListener(Listener<Double> listener)
    {
        mPLLFrequencyListener = listener;
    }

    /**
     * Regsiters the listener to receive complex sample buffers that have been filtered with automatic gain control applied
     */
    public void setFilteredGainAppliedComplexBufferListener(Listener<ReusableComplexBuffer> listener)
    {
        mFilteredGainAppliedComplexBufferListener = listener;
    }
}
