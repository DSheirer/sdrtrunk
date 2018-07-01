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
import io.github.dsheirer.sample.complex.Complex;

public class DQPSKGardnerDemodulator extends PSKDemodulator<Dibit>
{
    protected DQPSKGardnerSymbolEvaluator mSymbolEvaluator = new DQPSKGardnerSymbolEvaluator();
    private Complex mPreviousCurrentSample = new Complex(0, 0);
    private Complex mPreviousMiddleSample = new Complex(0, 0);
    private Complex mMiddleSymbol = new Complex(0, 0);
    protected Complex mCurrentSymbol = new Complex(0, 0);

    /**
     * Implements a Differential QPSK demodulator using a Costas Loop (PLL) and a Gardner timing error detector.
     *
     * This decoder is optimized for P25 Linear Simulcast Modulation (LSM).
     *
     * @param phaseLockedLoop for tracking carrier frequency error
     * @param interpolatingSampleBuffer to hold samples for interpolating a symbol
     */
    public DQPSKGardnerDemodulator(IPhaseLockedLoop phaseLockedLoop, InterpolatingSampleBuffer interpolatingSampleBuffer)
    {
        super(interpolatingSampleBuffer, phaseLockedLoop);
    }

	public void dispose()
	{
	}

    @Override
    protected void calculateSymbol()
    {
        //Note: the interpolating sample buffer holds 2 symbols worth of samples and the current sample method points
        //to the sample at the mid-point between those 2 symbol periods and the middle sample method points to the
        //sample that is half a symbol period after the current sample.  Since we need a middle sample and a current
        //symbol sample for the gardner calculation, we'll treat the interpolating buffer's current sample as the
        //gardner mid-point and we'll treat the interpolating buffer's mid-point sample as the current symbol
        //sample (ie flip-flopped)
        Complex middleSample = getInterpolatingSampleBuffer().getCurrentSample();
        Complex currentSample = getInterpolatingSampleBuffer().getMiddleSample();

        //Differential decode middle and current symbols by calculating the angular rotation between the previous and
        //current samples (current sample x complex conjugate of previous sample).
        mMiddleSymbol.setInphase(Complex.multiplyInphase(middleSample.inphase(), middleSample.quadrature(),
            mPreviousMiddleSample.inphase(), -mPreviousMiddleSample.quadrature()));
        mMiddleSymbol.setQuadrature(Complex.multiplyQuadrature(middleSample.inphase(), middleSample.quadrature(),
            mPreviousMiddleSample.inphase(), -mPreviousMiddleSample.quadrature()));

        mCurrentSymbol.setInphase(Complex.multiplyInphase(currentSample.inphase(), currentSample.quadrature(),
            mPreviousCurrentSample.inphase(), -mPreviousCurrentSample.quadrature()));
        mCurrentSymbol.setQuadrature(Complex.multiplyQuadrature(currentSample.inphase(), currentSample.quadrature(),
            mPreviousCurrentSample.inphase(), -mPreviousCurrentSample.quadrature()));

        //Set gain to unity before we calculate the error value
        mMiddleSymbol.normalize();
        mCurrentSymbol.normalize();

        //Pass symbols to evaluator to determine timing and phase error and make symbol decision
        mSymbolEvaluator.setSymbols(mMiddleSymbol, mCurrentSymbol);

        //Update symbol timing error
        getInterpolatingSampleBuffer().resetAndAdjust(mSymbolEvaluator.getTimingError());

        //Update PLL phase error
        getPLL().adjust(mSymbolEvaluator.getPhaseError());

        //Store current samples/symbols for next symbol calculation
        mPreviousMiddleSample.setValues(middleSample);
        mPreviousCurrentSample.setValues(currentSample);

        broadcast(mSymbolEvaluator.getSymbolDecision());
    }
}
