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
    protected InterpolatingSampleBuffer mInterpolatingSampleBuffer;
    protected IPhaseLockedLoop mPLL;
    protected IQPSKSymbolDecoder mDQPSKSymbolDecoder;
    private ISymbolPhaseErrorCalculator mSymbolPhaseErrorCalculator;
    private DQPSKTimingErrorDetector mDQPSKTimingErrorDetector = new DQPSKTimingErrorDetector();

    private Complex mReceivedSample = new Complex(0, 0);
    private Complex mPreviousPrecedingSample = new Complex(0, 0);
    private Complex mPreviousCurrentSample = new Complex(0, 0);
    private Complex mPrecedingSample = new Complex(0, 0);
    private Complex mCurrentSample = new Complex(0, 0);
    private Complex mPrecedingSymbol = new Complex(0, 0);
    protected Complex mCurrentSymbol = new Complex(0, 0);

    protected float mPhaseError;
    private float mSymbolTimingError;
    private Listener<Dibit> mDibitListener;

    /**
     * Decoder for Differential Quaternary Phase Shift Keying (DQPSK).  This decoder uses both a Costas Loop (PLL) and
     * a custom DQPSK symbol timing error detector to automatically align to the incoming carrier frequency and to
     * adjust for any changes in symbol timing.
     *
     * This detector is optimized for constant amplitude DQPSK symbols like C4FM.
     *
     * @param phaseLockedLoop for tracking carrier frequency error
     * @param symbolPhaseErrorCalculator to calculate symbol phase errors to apply against the PLL
     * @param symbolDecoder to decode demodulated symbols into dibits
     */
    public DQPSKDemodulator(IPhaseLockedLoop phaseLockedLoop, ISymbolPhaseErrorCalculator symbolPhaseErrorCalculator,
                            IQPSKSymbolDecoder symbolDecoder, InterpolatingSampleBuffer interpolatingSampleBuffer)
    {
        mPLL = phaseLockedLoop;
        mSymbolPhaseErrorCalculator = symbolPhaseErrorCalculator;
        mDQPSKSymbolDecoder = symbolDecoder;
        mInterpolatingSampleBuffer = interpolatingSampleBuffer;
    }

    /**
     * Processes a (filtered) buffer containing complex samples for decoding
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
     * Processes a complex sample for decoding.  Once sufficient samples are buffered, a symbol decision is made.
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

        //Store the sample in the interpolating buffer
        mInterpolatingSampleBuffer.receive(mReceivedSample);

        //Calculate the symbol once we've stored enough samples
        if(mInterpolatingSampleBuffer.hasSymbol())
        {
            calculateSymbol();
        }
    }

    /**
     * Calculates a symbol from the interpolating buffer
     */
    protected void calculateSymbol()
    {
        //Get preceding sample and an interpolated current sample from the interpolating buffer
        mPrecedingSample = mInterpolatingSampleBuffer.getPrecedingSample();
        mCurrentSample = mInterpolatingSampleBuffer.getCurrentSample();

        //Calculate preceding, current and following symbols as the delta rotation compared to the previous samples
        mPrecedingSymbol.setInphase(Complex.multiplyInphase(mPrecedingSample.inphase(), mPrecedingSample.quadrature(),
            mPreviousPrecedingSample.inphase(), -mPreviousPrecedingSample.quadrature()));
        mPrecedingSymbol.setQuadrature(Complex.multiplyQuadrature(mPrecedingSample.inphase(), mPrecedingSample.quadrature(),
            mPreviousPrecedingSample.inphase(), -mPreviousPrecedingSample.quadrature()));

        mCurrentSymbol.setInphase(Complex.multiplyInphase(mCurrentSample.inphase(), mCurrentSample.quadrature(),
            mPreviousCurrentSample.inphase(), -mPreviousCurrentSample.quadrature()));
        mCurrentSymbol.setQuadrature(Complex.multiplyQuadrature(mCurrentSample.inphase(), mCurrentSample.quadrature(),
            mPreviousCurrentSample.inphase(), -mPreviousCurrentSample.quadrature()));

        //Set gain to unity before we calculate the error value
        mPrecedingSymbol.normalize();
        mCurrentSymbol.normalize();

        //Symbol timing error calculation
        mSymbolTimingError = mDQPSKTimingErrorDetector.getError(mPrecedingSymbol, mCurrentSymbol);

        mSymbolTimingError = clip(mSymbolTimingError, 0.5f);
        mInterpolatingSampleBuffer.resetAndAdjust(mSymbolTimingError);

        //Store current samples/symbols to use for the next period
        mPreviousPrecedingSample.setValues(mPrecedingSample);
        mPreviousCurrentSample.setValues(mCurrentSample);

        //Calculate the phase error of the current symbol relative to the expected constellation and provide
        //feedback to the PLL
        mPhaseError = mSymbolPhaseErrorCalculator.getPhaseError(mCurrentSymbol);

        mPLL.adjust(mPhaseError);

        if(mDibitListener != null)
        {
            mDibitListener.receive(mDQPSKSymbolDecoder.decode(mCurrentSymbol));
        }
    }

    /**
     * Registers the listener to receive dibit symbol decisions from this demodulator
     */
    public void setDibitListener(Listener<Dibit> listener)
    {
        mDibitListener = listener;
    }

    /**
     * Constrains value to the range of ( -maximum <> maximum )
     */
    public static float clip(float value, float maximum)
    {
        if(value > maximum)
        {
            return maximum;
        }
        else if(value < -maximum)
        {
            return -maximum;
        }

        return value;
    }
}
