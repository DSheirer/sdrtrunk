/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.dsp.psk;

import io.github.dsheirer.dsp.filter.interpolator.Interpolator;
import io.github.dsheirer.dsp.filter.interpolator.InterpolatorFactory;
import io.github.dsheirer.dsp.psk.pll.CostasLoop;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.sample.complex.Complex;
import java.text.DecimalFormat;

public class DQPSKDecisionDirectedDemodulator extends PSKDemodulator<Dibit>
{
    protected DQPSKDecisionDirectedSymbolEvaluator mSymbolEvaluator = new DQPSKDecisionDirectedSymbolEvaluator();
    private Complex mPreviousPrecedingSample = new Complex(0, 0);
    private Complex mPreviousCurrentSample = new Complex(0, 0);
    private Complex mPrecedingSample = new Complex(0, 0);
    private Complex mCurrentSample = new Complex(0, 0);
    private Complex mPrecedingSymbol = new Complex(0, 0);
    protected Complex mCurrentSymbol = new Complex(0, 0);

    private final Interpolator mInterpolator = InterpolatorFactory.getInterpolator();
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("+#0.00000;-#0.00000");
    private float[] mI = new float[15];
    private float[] mQ = new float[15];

    /**
     * Decoder for Differential Quaternary Phase Shift Keying (DQPSK).  This decoder uses both a Costas Loop (PLL) and
     * a decision-directed DQPSK symbol timing and phase error detector to automatically align to the incoming carrier
     * frequency and to adjust for any changes in symbol timing.
     *
     * This detector is optimized for constant amplitude DQPSK symbols like C4FM.
     *
     * @param phaseLockedLoop for tracking carrier frequency error
     * @param interpolatingSampleBuffer to hold samples for interpolating a symbol
     */
    public DQPSKDecisionDirectedDemodulator(IPhaseLockedLoop phaseLockedLoop, InterpolatingSampleBuffer interpolatingSampleBuffer)
    {
        super(interpolatingSampleBuffer, phaseLockedLoop);
    }

    @Override
    public void receive(float inphase, float quadrature)
    {
        super.receive(inphase, quadrature);

        System.arraycopy(mI, 1, mI, 0, mI.length - 1);
        System.arraycopy(mQ, 1, mQ, 0, mQ.length - 1);

        //Rotate the sample and add to buffer
        Complex current = new Complex(inphase, quadrature);
        current.normalizeFast();
        current.multiply(getPLL().getCurrentVector());
        mI[mI.length - 1] = current.inphase();
        mQ[mQ.length - 1] = current.quadrature();
        float iTemp = mInterpolator.filter(mI, 6, .416f);
        float qTemp = mInterpolator.filter(mQ, 6, .416f);
        float iSymbol = (mI[0] * iTemp) - (-mQ[0] * qTemp);
        float qSymbol = (mI[0] * qTemp) + (iTemp * -mQ[0]);
        float radians = (float)Math.atan2(iSymbol, qSymbol);

        StringBuilder sb = new StringBuilder();
        sb.append(DECIMAL_FORMAT.format(radians)).append(",");
        sb.append(DECIMAL_FORMAT.format(((CostasLoop)getPLL()).getLoopFrequency()));
//        System.out.println(sb);
    }

    /**
     * Calculates a symbol from the interpolating buffer
     */
    protected void calculateSymbol()
    {
        //Get preceding sample and an interpolated current sample from the interpolating buffer
        mPrecedingSample = getInterpolatingSampleBuffer().getPrecedingSample();
        mCurrentSample = getInterpolatingSampleBuffer().getCurrentSample();

        //Differential decode preceding and current symbols by calculating the angular rotation between the previous and
        //current samples (current sample x complex conjugate of previous sample).

        //Note: preceding symbol is a preceding measurement of the current symbol that is simply used as a reference
        //point to determine vector rotation to the current symbol -- it is not the true predecessor symbol
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

        //Apply symbols to evaluator to calculate phase and timing error and make a symbol decision
        mSymbolEvaluator.setSymbol(mPrecedingSymbol, mCurrentSymbol);

        //Update the symbol timing error
        getInterpolatingSampleBuffer().resetAndAdjust(mSymbolEvaluator.getTimingError());

        //Update the costas loop (PLL) with any measured phase error
//        getPLL().adjust(clip(mSymbolEvaluator.getPhaseError(), 0.5f));

        //Store current samples/symbols to use for the next symbol period
        mPreviousPrecedingSample.setValues(mPrecedingSample);
        mPreviousCurrentSample.setValues(mCurrentSample);

        broadcast(mSymbolEvaluator.getSymbolDecision());
    }
}
