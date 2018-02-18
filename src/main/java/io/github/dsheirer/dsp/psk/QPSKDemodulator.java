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
package io.github.dsheirer.dsp.psk;

import io.github.dsheirer.dsp.psk.pll.CostasLoop;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;
import io.github.dsheirer.sample.complex.ComplexBuffer;
import io.github.dsheirer.sample.complex.ComplexSampleListener;

public class QPSKDemodulator implements ComplexSampleListener
{
    private InterpolatingSymbolBuffer mInterpolatingSymbolBuffer;
    private IPhaseLockedLoop mPLL;
    private ISymbolPhaseErrorCalculator mSymbolPhaseErrorCalculator;
    private IQPSKSymbolDecoder mQPSKSymbolDecoder;
    private GardnerDetector mGardnerDetector = new GardnerDetector();
    private Listener<Complex> mSymbolListener;
    private Listener<Double> mPLLErrorListener;
    private Listener<Double> mPLLFrequencyListener;
    private Listener<Dibit> mDibitListener;
    private Listener<SymbolDecisionData> mSymbolDecisionDataListener;

    private Complex mPreviousSample = new Complex(0, 0);
    private Complex mPreviousMiddleSample = new Complex(0, 0);
    private Complex mCurrentSample = new Complex(0, 0);
    private Complex mMiddleSample = new Complex(0, 0);
    private Complex mPreviousSymbol = new Complex(0, 0);
    private Complex mMiddleSymbol = new Complex(0, 0);
    private Complex mCurrentSymbol = new Complex(0, 0);
    private float mPhaseError;
    private float mSymbolTimingError;
    private double mSampleRate;

    /**
     * Decoder for Quaternary Phase Shift Keying (QPSK) and Differential QPSK (DQPSK).  This decoder uses both a
     * phase-locked loop and a gardner symbol timing error detector to automatically align to the incoming carrier
     * frequency and to adjust for any changes in symbol timing.
     *
     * @param phaseLockedLoop for tracking carrier frequency error
     * @param symbolPhaseErrorCalculator to calculate symbol phase errors and optionally adjust for differential encoding
     * @param symbolDecoder to decode demodulated symbols into dibits
     * @param sampleRate of the incoming complex sample stream
     * @param symbolRate of the decoded QPSK symbols.
     */
    public QPSKDemodulator(IPhaseLockedLoop phaseLockedLoop, ISymbolPhaseErrorCalculator symbolPhaseErrorCalculator,
                           IQPSKSymbolDecoder symbolDecoder, double sampleRate, double symbolRate)
    {
        if(sampleRate < (symbolRate * 2))
        {
            throw new IllegalArgumentException("Sample rate [" + sampleRate +
                "] must be at least 2 x symbol rate [" + symbolRate + "]");
        }

        mSampleRate = sampleRate;
        mPLL = phaseLockedLoop;
        mSymbolPhaseErrorCalculator = symbolPhaseErrorCalculator;
        mQPSKSymbolDecoder = symbolDecoder;

        float samplesPerSymbol = (float)(sampleRate / symbolRate);
        mInterpolatingSymbolBuffer = new InterpolatingSymbolBuffer(samplesPerSymbol);
    }

    /**
     * Submits a buffer containing complex samples for decoding
     * @param complexBuffer with complex samples
     */
    public void receive(ComplexBuffer complexBuffer)
    {
        float[] samples = complexBuffer.getSamples();

        for(int x = 0; x < samples.length; x += 2)
        {
            receive(samples[x], samples[x + 1]);
        }
    }

    /**
     * Submits a complex sample for decoding.
     * @param inphase value for the sample
     * @param quadrature value for the sample
     */
    @Override
    public void receive(float inphase, float quadrature)
    {
        //Update current sample with values
        mCurrentSample.setValues(inphase, quadrature);

        //Mix current sample with costas loop to remove any rotation that is present from a mis-tuned carrier frequency
        mCurrentSample.multiply(mPLL.incrementAndGetCurrentVector());

        mInterpolatingSymbolBuffer.receive(mCurrentSample);

        //Calculate the symbol once we've stored enough samples
        if(mInterpolatingSymbolBuffer.hasSymbol())
        {
            //Get middle and current samples from the interpolating buffer
            mMiddleSample = mInterpolatingSymbolBuffer.getMiddleSample();
            mCurrentSample = mInterpolatingSymbolBuffer.getCurrentSample();

            //Eye diagram listener
            if(mSymbolDecisionDataListener != null)
            {
                mSymbolDecisionDataListener.receive(mInterpolatingSymbolBuffer.getSymbolDecisionData());
            }

            //Calculate middle and current symbols as the delta between the previous and current samples
            mMiddleSymbol.setInphase(Complex.multiplyInphase(mMiddleSample.inphase(), mMiddleSample.quadrature(), mPreviousMiddleSample.inphase(), -mPreviousMiddleSample.quadrature()));
            mMiddleSymbol.setQuadrature(Complex.multiplyQuadrature(mMiddleSample.inphase(), mMiddleSample.quadrature(), mPreviousMiddleSample.inphase(), -mPreviousMiddleSample.quadrature()));
            mCurrentSymbol.setInphase(Complex.multiplyInphase(mCurrentSample.inphase(), mCurrentSample.quadrature(), mPreviousSample.inphase(), -mPreviousSample.quadrature()));
            mCurrentSymbol.setQuadrature(Complex.multiplyQuadrature(mCurrentSample.inphase(), mCurrentSample.quadrature(), mPreviousSample.inphase(), -mPreviousSample.quadrature()));

            //Set gain to unity before we calculate the error value
            mMiddleSymbol.normalize();
            mCurrentSymbol.normalize();

            //Symbol timing error calculations
            mSymbolTimingError = mGardnerDetector.getError(mPreviousSymbol, mMiddleSymbol, mCurrentSymbol);
            mInterpolatingSymbolBuffer.resetAndAdjust(mSymbolTimingError);

            //Store current samples/symbols to use for the next period
            mPreviousSample.setValues(mCurrentSample);
            mPreviousMiddleSample.setValues(mMiddleSample);
            mPreviousSymbol.setValues(mCurrentSymbol);

            //Send to an external constellation symbol listener when registered
            if(mSymbolListener != null)
            {
                mSymbolListener.receive(mCurrentSymbol);
            }

            //Adjust the current symbol as necessary (e.g. for differential encoding)
            mSymbolPhaseErrorCalculator.adjust(mCurrentSymbol);

            //Calculate the phase error of the current symbol relative to the expected constellation and provide
            //feedback to the PLL
            mPhaseError = mSymbolPhaseErrorCalculator.getPhaseError(mCurrentSymbol);

            mPhaseError = GardnerDetector.clip(mPhaseError, 0.2f);
            mPLL.adjust(mPhaseError);

            if(mPLLErrorListener != null)
            {
                mPLLErrorListener.receive((double)mPhaseError);
            }

            if(mPLLFrequencyListener != null)
            {
                double loopFrequency = ((CostasLoop)mPLL).getLoopFrequency();

                loopFrequency *= mSampleRate / (2.0 * Math.PI);

                mPLLFrequencyListener.receive(loopFrequency);
            }

            //Decode the dibit from the symbol and send to the listener
            if(mDibitListener != null)
            {
                mDibitListener.receive(mQPSKSymbolDecoder.decode(mCurrentSymbol));
            }

            //TODO: Assemble dibits here and broadcast
        }
    }

    /**
     * Registers the listener to receive decoded QPSK symbols
     */
    public void setSymbolListener(Listener<Complex> listener)
    {
        mSymbolListener = listener;
    }

    /**
     * Registers the listener to receive decoded QPSK dibits
     */
    public void setDibitListener(Listener<Dibit> listener)
    {
        mDibitListener = listener;
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

    public void setSymbolDecisionDataListener(Listener<SymbolDecisionData> listener)
    {
        mSymbolDecisionDataListener = listener;
    }

}
