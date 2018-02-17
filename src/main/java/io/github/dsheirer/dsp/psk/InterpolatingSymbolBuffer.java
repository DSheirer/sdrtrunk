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

import io.github.dsheirer.dsp.filter.interpolator.RealInterpolator;
import io.github.dsheirer.sample.complex.Complex;

public class InterpolatingSymbolBuffer
{
    private Complex mMiddleSample = new Complex(0,0);
    private Complex mCurrentSample = new Complex(0,0);

    private float[] mDelayLineInphase;
    private float[] mDelayLineQuadrature;
    private float mMu; //Sample Counter
    private float mGainMu = 0.05f; //Sample Counter Gain
    private float mOmega;
    private float mGainOmega = 0.1f * mGainMu * mGainMu;
    private float mOmegaRel = 0.005f;
    private float mOmegaMid;

    private int mDelayLinePointer = 0;
    private int mTwiceSamplesPerSymbol;
    private RealInterpolator mInterpolator = new RealInterpolator(1.0f);

    public InterpolatingSymbolBuffer(float samplesPerSymbol)
    {
        mMu = samplesPerSymbol;
        mTwiceSamplesPerSymbol = (int)Math.floor(2.0 * samplesPerSymbol);
        mDelayLineInphase = new float[2 * mTwiceSamplesPerSymbol];
        mDelayLineQuadrature = new float[2 * mTwiceSamplesPerSymbol];
        mOmega = samplesPerSymbol;
        mOmegaMid = samplesPerSymbol;

    }

    public void receive(Complex currentSample)
    {
        mMu--;

        //Fill up the delay line to use with the interpolator
        mDelayLineInphase[mDelayLinePointer] = currentSample.inphase();
        mDelayLineInphase[mDelayLinePointer + mTwiceSamplesPerSymbol] = currentSample.inphase();
        mDelayLineQuadrature[mDelayLinePointer] = currentSample.quadrature();
        mDelayLineQuadrature[mDelayLinePointer + mTwiceSamplesPerSymbol] = currentSample.quadrature();

        //Increment pointer and keep pointer in bounds
        mDelayLinePointer = (mDelayLinePointer + 1) % mTwiceSamplesPerSymbol;
    }

    /**
     * Updates the internal sample counter with additional samples to add to the symbol sample counter.
     * @param samplesToAdd to the sample counter
     */
    public void increaseSampleCounter(float samplesToAdd)
    {
        mMu += samplesToAdd;
    }

    /**
     * Adjusts samples per symbol and symbol timing counters and increments the sample counter to collect another
     * symbol.
     *
     * @param symbolTimingError from a symbol timing error detector
     */
    public void resetAndAdjust(float symbolTimingError)
    {
        //mOmega is samples per symbol and is constrained to floating between +/- .005 of the nominal samples per
        //symbol
        mOmega = mOmega + mGainOmega * symbolTimingError;
        mOmega = mOmegaMid + clip(mOmega - mOmegaMid, mOmegaRel);

        //Add another symbol's worth of samples to the counter and adjust timing based on gardner error
        increaseSampleCounter((mOmega + (mGainMu * symbolTimingError)));
    }

    /**
     * Indicates if this buffer has accumulated a enough samples to represent a full symbol
     */
    public boolean hasSymbol()
    {
        return mMu < 1.0f;
    }

    /**
     * Interpolated middle sample for the symbol.  Note: this method should only be invoked after testing for a
     * complete symbol with the hasSymbol() method.
     */
    public Complex getMiddleSample()
    {
        /* Calculate interpolated middle sample and current sample */
        mMiddleSample.setValues(getInphase(mMu), getQuadrature(mMu));

        return mMiddleSample;
    }

    /**
     * Interpolated current sample for the symbol.  This sample represents the ideal symbol sampling point.
     * Note: this method should only be invoked after testing for a complete symbol with the hasSymbol() method.
     */
    public Complex getCurrentSample()
    {
        float half_omega = mOmega / 2.0f;
        int half_sps = (int)Math.floor(half_omega);
        float half_mu = mMu + half_omega - half_sps;

        if(half_mu > 1.0)
        {
            half_mu -= 1.0;
            half_sps += 1;
        }

        mCurrentSample.setValues(getInphase(half_mu, half_sps), getQuadrature(half_mu, half_sps));

        return mCurrentSample;
    }

    /**
     * Returns the interpolated inphase value for the specified offset
     * @param interpolation into the buffer to calculate the interpolated sample
     * @return inphase value of the interpolated sample
     */
    public float getInphase(float interpolation)
    {
        return mInterpolator.filter(mDelayLineInphase, mDelayLinePointer, interpolation);
    }

    /**
     * Returns the interpolated inphase value for the specified offset
     * @param interpolation into the buffer to calculate the interpolated sample
     * @return inphase value of the interpolated sample
     */
    public float getInphase(float interpolation, int indexOffet)
    {
        return mInterpolator.filter(mDelayLineInphase, mDelayLinePointer + indexOffet, interpolation);
    }

    /**
     * Returns the interpolated quadrature value for the specified offset
     * @param interpolation into the buffer to calculate the interpolated sample
     * @return quadrature value of the interpolated sample
     */
    public float getQuadrature(float interpolation)
    {
        return mInterpolator.filter(mDelayLineQuadrature, mDelayLinePointer, interpolation);
    }

    /**
     * Returns the interpolated quadrature value for the specified offset
     * @param interpolation into the buffer to calculate the interpolated sample
     * @return quadrature value of the interpolated sample
     */
    public float getQuadrature(float interpolation, int indexOffset)
    {
        return mInterpolator.filter(mDelayLineQuadrature, mDelayLinePointer + indexOffset, interpolation);
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
