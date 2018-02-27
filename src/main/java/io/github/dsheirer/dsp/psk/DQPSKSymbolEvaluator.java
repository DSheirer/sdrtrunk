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

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.sample.complex.Complex;

public class DQPSKSymbolEvaluator implements IDQPSKSymbolEvaluator
{
    private static final Complex ROTATE_FROM_PLUS_135 = Complex.fromAngle(-3.0 * Math.PI / 4.0);
    private static final Complex ROTATE_FROM_PLUS_45 = Complex.fromAngle(-1.0 * Math.PI / 4.0);
    private static final Complex ROTATE_FROM_MINUS_45 = Complex.fromAngle(1.0 * Math.PI / 4.0);
    private static final Complex ROTATE_FROM_MINUS_135 = Complex.fromAngle(3.0 * Math.PI / 4.0);

    private Complex mSymbolError = new Complex(0,0);
    private float mPhaseError = 0.0f;
    private float mTimingError = 0.0f;
    private float mTimingErrorSign = 1.0f;
    private Dibit mSymbolDecision = Dibit.D00_PLUS_1;

    public DQPSKSymbolEvaluator()
    {
    }

    /**
     * Sets the symbol to be evaluated and calculates the symbol decision and phase and timing errors of the symbol
     * relative to the four QPSK reference symbol locations.  After invoking this method, you can access the phase and
     * timing errors and the symbol decision via their respective accessor methods.
     *
     * Phase and timing error values are calculated by determining the symbol's quadrant and multiplying the symbol
     * by the complex conjugate of the reference symbol for that quadrant and then deriving the radian angle error
     * value.  Both phase and timing error use this angle error, however the timing error is corrected with the
     * appropriate sign so that the error value indicates the correct error direction.
     *
     * @param complex symbol to be evaluated
     */
    @Override
    public void setSymbol(Complex complex)
    {
        mSymbolError.setValues(complex);

        if(mSymbolError.quadrature() > 0.0f)
        {
            if(mSymbolError.inphase() > 0.0f)
            {
                mSymbolDecision = Dibit.D00_PLUS_1;
                mSymbolError.multiply(ROTATE_FROM_PLUS_45);
            }
            else
            {
                mSymbolDecision = Dibit.D01_PLUS_3;
                mSymbolError.multiply(ROTATE_FROM_PLUS_135);
            }

            mTimingErrorSign = 1.0f;
        }
        else
        {
            if(mSymbolError.inphase() > 0.0f)
            {
                mSymbolDecision = Dibit.D10_MINUS_1;
                mSymbolError.multiply(ROTATE_FROM_MINUS_45);
            }
            else
            {
                mSymbolDecision = Dibit.D11_MINUS_3;
                mSymbolError.multiply(ROTATE_FROM_MINUS_135);
            }

            mTimingErrorSign = -1.0f;
        }

        //Since we've rotated the error symbol back to 0 radians, the quadrature value closely approximates the
        //arctan of the error angle relative to 0 radians and this provides our error value
        mPhaseError = -mSymbolError.quadrature();

        //Timing error is the same as phase error with the sign corrected according to the symbol's hemisphere
        mTimingError = mPhaseError * mTimingErrorSign;
    }

    /**
     * Phase error of the symbol relative to the nearest reference symbol.
     * @return phase error in radians of distance from the reference symbol.
     */
    @Override
    public float getPhaseError()
    {
        return mPhaseError;
    }

    /**
     * Timing error of the symbol relative to the nearest reference symbol.
     *
     * @return timing error in radians of angular distance from the reference symbol recognizing that the symbol
     * originates at zero radians and rotates toward the intended reference symbol, therefore the error value indicates
     * if the symbol was sampled early (-) or late (+) relative to the reference symbol.
     */
    @Override
    public float getTimingError()
    {
        return mTimingError;
    }

    /**
     * Reference symbol that is closest to the transmitted/sampled symbol.
     */
    @Override
    public Dibit getSymbolDecision()
    {
        return mSymbolDecision;
    }
}
