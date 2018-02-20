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
    private float mSamplingPoint; //Sample Counter
    private float mSampleCounterGain = 0.05f; //Sample Counter Gain
    private float mSamplesPerSymbol;
    private float mDetectedSamplesPerSymbol;
    private float mDetectedSamplesPerSymbolGain = 0.1f * mSampleCounterGain * mSampleCounterGain;
    private float mMaxDeviation = 0.005f;

    private int mDelayLinePointer = 0;
    private int mTwiceSamplesPerSymbol;
    private RealInterpolator mInterpolator = new RealInterpolator(1.0f);
    private SymbolDecisionData2 mSymbolDecisionData2;

    public InterpolatingSymbolBuffer(float samplesPerSymbol)
    {
        mSamplingPoint = samplesPerSymbol;
        mTwiceSamplesPerSymbol = (int)Math.floor(2.0 * samplesPerSymbol);
        mDelayLineInphase = new float[2 * mTwiceSamplesPerSymbol];
        mDelayLineQuadrature = new float[2 * mTwiceSamplesPerSymbol];
        mDetectedSamplesPerSymbol = samplesPerSymbol;
        mSamplesPerSymbol = samplesPerSymbol;

        mSymbolDecisionData2 = new SymbolDecisionData2((int)samplesPerSymbol);
    }

    public void receive(Complex currentSample)
    {
        mSymbolDecisionData2.receive(currentSample);
        mSamplingPoint--;

        //Fill up the delay line to use with the interpolator
        mDelayLineInphase[mDelayLinePointer] = currentSample.inphase();
        mDelayLineInphase[mDelayLinePointer + mTwiceSamplesPerSymbol] = currentSample.inphase();
        mDelayLineQuadrature[mDelayLinePointer] = currentSample.quadrature();
        mDelayLineQuadrature[mDelayLinePointer + mTwiceSamplesPerSymbol] = currentSample.quadrature();

        //Increment pointer and keep pointer in bounds
        mDelayLinePointer++;
        mDelayLinePointer = mDelayLinePointer % mTwiceSamplesPerSymbol;

    }

    /**
     * Updates the internal sample counter with additional samples to add to the symbol sample counter.
     * @param samplesToAdd to the sample counter
     */
    public void increaseSampleCounter(float samplesToAdd)
    {
        mSamplingPoint += samplesToAdd;
    }

    /**
     * Adjusts samples per symbol and symbol timing counters and increments the sample counter to collect another
     * symbol.
     *
     * @param symbolTimingError from a symbol timing error detector
     */
    public void resetAndAdjust(float symbolTimingError)
    {
        //mDetectedSamplesPerSymbol is samples per symbol and is constrained to floating between +/- .005 of the nominal samples per
        //symbol
        mDetectedSamplesPerSymbol = mDetectedSamplesPerSymbol + mDetectedSamplesPerSymbolGain * symbolTimingError;
        mDetectedSamplesPerSymbol = mSamplesPerSymbol + clip(mDetectedSamplesPerSymbol - mSamplesPerSymbol, mMaxDeviation);

        //Add another symbol's worth of samples to the counter and adjust timing based on gardner error
        increaseSampleCounter((mDetectedSamplesPerSymbol + (mSampleCounterGain * symbolTimingError)));
    }

    /**
     * Indicates if this buffer has accumulated a enough samples to represent a full symbol
     */
    public boolean hasSymbol()
    {
        return mSamplingPoint < 1.0f;
    }

    /**
     * Interpolated middle sample for the symbol.  Note: this method should only be invoked after testing for a
     * complete symbol with the hasSymbol() method.
     */
    public Complex getMiddleSample()
    {
        /* Calculate interpolated middle sample and current sample */
        mMiddleSample.setValues(getInphase(mSamplingPoint), getQuadrature(mSamplingPoint));

        return mMiddleSample;
    }

    /**
     * Interpolated current sample for the symbol.  This sample represents the ideal symbol sampling point.
     * Note: this method should only be invoked after testing for a complete symbol with the hasSymbol() method.
     */
    public Complex getCurrentSample()
    {
        float halfDetectedSamplesPerSymbol = mDetectedSamplesPerSymbol / 2.0f;
        int halfSamplingPointOffset = (int)Math.floor(halfDetectedSamplesPerSymbol);
        float halfSamplingPoint = mSamplingPoint + halfDetectedSamplesPerSymbol - halfSamplingPointOffset;

        if(halfSamplingPoint > 1.0)
        {
            halfSamplingPoint -= 1.0;
            halfSamplingPointOffset += 1;
        }

        mCurrentSample.setValues(getInphase(halfSamplingPointOffset, halfSamplingPoint),
                                 getQuadrature(halfSamplingPointOffset, halfSamplingPoint));

        return mCurrentSample;
    }

    /**
     * Contents of the interpolating buffer and the current buffer index and symbol decision offset.  This data can
     * be used to support an external eye-diagram chart.
     * @return symbol decision data.
     */
    public SymbolDecisionData2 getSymbolDecisionData()
    {
        mSymbolDecisionData2.setSamplingPoint(mSamplingPoint);
        return mSymbolDecisionData2;
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
     * @param delayLineOffset offset into the inphase delay line buffer
     * @param interpolation into the buffer to calculate the interpolated sample
     * @return inphase value of the interpolated sample
     */
    public float getInphase(int delayLineOffset, float interpolation)
    {
        return mInterpolator.filter(mDelayLineInphase, mDelayLinePointer + delayLineOffset, interpolation);
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
     * @param delayLineOffset offset into the quadrature delay line buffer
     * @param interpolation into the buffer to calculate the interpolated sample
     * @return quadrature value of the interpolated sample
     */
    public float getQuadrature(int delayLineOffset, float interpolation)
    {
        return mInterpolator.filter(mDelayLineQuadrature, mDelayLinePointer + delayLineOffset, interpolation);
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
