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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DQPSKDemodulator implements ComplexSampleListener
{
    private final static Logger mLog = LoggerFactory.getLogger(DQPSKDemodulator.class);
    private InterpolatingSymbolBuffer2 mInterpolatingSymbolBuffer;
    private IPhaseLockedLoop mPLL;
    private ISymbolPhaseErrorCalculator mSymbolPhaseErrorCalculator;
    private IQPSKSymbolDecoder mDQPSKSymbolDecoder;
//    private GardnerDetector mGardnerDetector = new GardnerDetector();
    private EarlyLateDetector mEarlyLateDetector = new EarlyLateDetector();
    private EarlyLateDetector2 mEarlyLateDetector2 = new EarlyLateDetector2();
    private Listener<Complex> mSymbolListener;
    private Listener<Double> mPLLErrorListener;
    private Listener<Double> mPLLFrequencyListener;
    private Listener<Double> mSamplesPerSymbolListener;
    private Listener<Dibit> mDibitListener;
    private Listener<SymbolDecisionData2> mSymbolDecisionDataListener;

    private Complex mReceivedSample = new Complex(0, 0);

    private Complex mPreviousPrecedingSample = new Complex(0, 0);
    private Complex mPreviousCurrentSample = new Complex(0, 0);
    private Complex mPreviousFollowingSample = new Complex(0, 0);

    private Complex mCurrentPrecedingSample = new Complex(0, 0);
    private Complex mCurrentCurrentSample = new Complex(0, 0);
    private Complex mCurrentFollowingSample = new Complex(0, 0);

    private Complex mPrecedingSymbol = new Complex(0, 0);
    private Complex mCurrentSymbol = new Complex(0, 0);
    private Complex mFollowingSymbol = new Complex(0, 0);

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
    public DQPSKDemodulator(IPhaseLockedLoop phaseLockedLoop, ISymbolPhaseErrorCalculator symbolPhaseErrorCalculator,
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
        mDQPSKSymbolDecoder = symbolDecoder;

        float samplesPerSymbol = (float)(sampleRate / symbolRate);
        mInterpolatingSymbolBuffer = new InterpolatingSymbolBuffer2(samplesPerSymbol);
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
        mReceivedSample.setValues(inphase, quadrature);

        //Mix current sample with costas loop to remove any rotation that is present from a mis-tuned carrier frequency
        mReceivedSample.multiply(mPLL.incrementAndGetCurrentVector());

        mInterpolatingSymbolBuffer.receive(mReceivedSample);

        //Calculate the symbol once we've stored enough samples
        if(mInterpolatingSymbolBuffer.hasSymbol())
        {
            //Get middle and current samples from the interpolating buffer
            mCurrentPrecedingSample = mInterpolatingSymbolBuffer.getPreceedingSample();
            mCurrentCurrentSample = mInterpolatingSymbolBuffer.getCurrentSample();
            mCurrentFollowingSample = mInterpolatingSymbolBuffer.getFollowingSample();

            //Eye diagram listener
            if(mSymbolDecisionDataListener != null)
            {
                mSymbolDecisionDataListener.receive(mInterpolatingSymbolBuffer.getSymbolDecisionData());
            }

            //Calculate preceding, current and following symbols as the delta rotation compared to the previous samples
            mPrecedingSymbol.setInphase(Complex.multiplyInphase(mCurrentPrecedingSample.inphase(), mCurrentPrecedingSample.quadrature(),
                mPreviousPrecedingSample.inphase(), -mPreviousPrecedingSample.quadrature()));
            mPrecedingSymbol.setQuadrature(Complex.multiplyQuadrature(mCurrentPrecedingSample.inphase(), mCurrentPrecedingSample.quadrature(),
                mPreviousPrecedingSample.inphase(), -mPreviousPrecedingSample.quadrature()));

            mCurrentSymbol.setInphase(Complex.multiplyInphase(mCurrentCurrentSample.inphase(), mCurrentCurrentSample.quadrature(),
                mPreviousCurrentSample.inphase(), -mPreviousCurrentSample.quadrature()));
            mCurrentSymbol.setQuadrature(Complex.multiplyQuadrature(mCurrentCurrentSample.inphase(), mCurrentCurrentSample.quadrature(),
                mPreviousCurrentSample.inphase(), -mPreviousCurrentSample.quadrature()));

            mFollowingSymbol.setInphase(Complex.multiplyInphase(mCurrentFollowingSample.inphase(), mCurrentFollowingSample.quadrature(),
                mPreviousFollowingSample.inphase(), -mPreviousFollowingSample.quadrature()));
            mFollowingSymbol.setQuadrature(Complex.multiplyQuadrature(mCurrentFollowingSample.inphase(), mCurrentFollowingSample.quadrature(),
                mPreviousFollowingSample.inphase(), -mPreviousFollowingSample.quadrature()));

            //Set gain to unity before we calculate the error value
            mPrecedingSymbol.normalize();
            mCurrentSymbol.normalize();
            mFollowingSymbol.normalize();

            //Send to an external constellation symbol listener when registered
            if(mSymbolListener != null)
            {
                mSymbolListener.receive(mCurrentSymbol);
            }

            //Symbol timing error calculations
            mSymbolTimingError = mEarlyLateDetector.getError(mPrecedingSymbol, mCurrentSymbol, mFollowingSymbol);

            mSymbolTimingError = GardnerDetector.clip(mSymbolTimingError, 0.5f);
            mInterpolatingSymbolBuffer.resetAndAdjust(mSymbolTimingError);

            if(mSamplesPerSymbolListener != null)
            {
                mSamplesPerSymbolListener.receive((double)mInterpolatingSymbolBuffer.getSamplingPoint());
            }

            //Store current samples/symbols to use for the next period
            mPreviousPrecedingSample.setValues(mCurrentPrecedingSample);
            mPreviousCurrentSample.setValues(mCurrentCurrentSample);
            mPreviousFollowingSample.setValues(mCurrentFollowingSample);

            //Calculate the phase error of the current symbol relative to the expected constellation and provide
            //feedback to the PLL
            mSymbolPhaseErrorCalculator.adjust(mCurrentSymbol);
            mPhaseError = mSymbolPhaseErrorCalculator.getPhaseError(mCurrentSymbol);

            mPhaseError = GardnerDetector.clip(mPhaseError, 0.15f);

            mPLL.adjust(mPhaseError);
//            mPLL.adjust(0.0);

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
                mDibitListener.receive(mDQPSKSymbolDecoder.decode(mCurrentSymbol));
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

    /**
     * Registers the listener to receive PLL error values
     */
    public void setSamplesPerSymbolListener(Listener<Double> listener)
    {
        mSamplesPerSymbolListener = listener;
    }

    public void setSymbolDecisionDataListener(Listener<SymbolDecisionData2> listener)
    {
        mSymbolDecisionDataListener = listener;
    }

}
