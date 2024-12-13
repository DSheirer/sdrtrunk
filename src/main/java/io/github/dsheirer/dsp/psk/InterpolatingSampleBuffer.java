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

import io.github.dsheirer.dsp.filter.interpolator.InterpolatorScalar;
import io.github.dsheirer.sample.complex.Complex;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterpolatingSampleBuffer
{
    private final static Logger mLog = LoggerFactory.getLogger(InterpolatingSampleBuffer.class);
    private static final float MAXIMUM_DEVIATION_SAMPLES_PER_SYMBOL = 0.02f; // +/- 2% deviation

    private Complex mPrecedingSample = new Complex(0,0);
    private Complex mCurrentSample = new Complex(0,0);
    private Complex mMiddleSample = new Complex(0,0);

    protected float[] mDelayLineInphase;
    protected float[] mDelayLineQuadrature;
    protected int mDelayLinePointer = 0;
    private int mTwiceSamplesPerSymbol;

    private float mSamplingPoint;
    private float mSampleCounterGain = 0.5f;
    private float mDetectedSamplesPerSymbol;
    private float mDetectedSamplesPerSymbolGain = 0.1f * mSampleCounterGain * mSampleCounterGain;
    private float mMaximumSamplesPerSymbol;
    private float mMinimumSamplesPerSymbol;

    private InterpolatorScalar mInterpolator = new InterpolatorScalar(1.0f);

    /**
     * Buffer to store complex sample data and produce interpolated samples.
     * @param samplesPerSymbol
     * @param sampleCounterGain for the symbol timing error adjustments
     */
    public InterpolatingSampleBuffer(float samplesPerSymbol, float sampleCounterGain)
    {
        mSamplingPoint = samplesPerSymbol;
        mDetectedSamplesPerSymbol = samplesPerSymbol;
        mMaximumSamplesPerSymbol = samplesPerSymbol * (1.0f + MAXIMUM_DEVIATION_SAMPLES_PER_SYMBOL);
        mMinimumSamplesPerSymbol = samplesPerSymbol * (1.0f - MAXIMUM_DEVIATION_SAMPLES_PER_SYMBOL);
        mTwiceSamplesPerSymbol = (int) FastMath.floor(2.0 * samplesPerSymbol);
        mDelayLineInphase = new float[2 * mTwiceSamplesPerSymbol];
        mDelayLineQuadrature = new float[2 * mTwiceSamplesPerSymbol];

        mSampleCounterGain = sampleCounterGain;
        mDetectedSamplesPerSymbolGain = 0.1f * mSampleCounterGain * mSampleCounterGain;
    }

    /**
     * Stores the sample in the buffer and updates pointers.
     * @param sample
     */
    public void receive(Complex sample)
    {
        mSamplingPoint--;

        //Fill up the delay line to use with the interpolator
        mDelayLineInphase[mDelayLinePointer] = sample.inphase();
        mDelayLineInphase[mDelayLinePointer + mTwiceSamplesPerSymbol] = sample.inphase();
        mDelayLineQuadrature[mDelayLinePointer] = sample.quadrature();
        mDelayLineQuadrature[mDelayLinePointer + mTwiceSamplesPerSymbol] = sample.quadrature();

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
        //Adjust detected samples per symbol based on timing error
        mDetectedSamplesPerSymbol = mDetectedSamplesPerSymbol + (symbolTimingError * mDetectedSamplesPerSymbolGain);

        //Constrain detected samples per symbol to min/max values
        if(mDetectedSamplesPerSymbol > mMaximumSamplesPerSymbol)
        {
            mDetectedSamplesPerSymbol = mMaximumSamplesPerSymbol;
        }

        if(mDetectedSamplesPerSymbol < mMinimumSamplesPerSymbol)
        {
            mDetectedSamplesPerSymbol = mMinimumSamplesPerSymbol;
        }

        //Add another symbol's worth of samples to the counter and adjust timing based on gardner error
        increaseSampleCounter((mDetectedSamplesPerSymbol + (symbolTimingError * mSampleCounterGain)));
    }

    /**
     * Current value of the detected samples per symbol
     */
    public float getSamplingPoint()
    {
        return mSamplingPoint;
    }

    /**
     * Indicates if this buffer has accumulated a enough samples to represent a full symbol
     */
    public boolean hasSymbol()
    {
        return mSamplingPoint < 1.0f;
    }

    /**
     * Un-interpolated sample that precedes the current interpolated sampling point.  The current interpolated sampling
     * point falls somewhere between sample index 3 and index 4 and the sample is interpolated from sample
     * indexes 0 - 7, therefore the uninterpolated sample that immediately precedes the current sample is
     * located at index 3.
     */
    public Complex getPrecedingSample()
    {
        mPrecedingSample.setValues(mDelayLineInphase[mDelayLinePointer + 3], mDelayLineQuadrature[mDelayLinePointer + 3]);
        return mPrecedingSample;
    }

    /**
     * Interpolated current sample for the symbol.
     *
     * Note: this method should only be invoked after testing for a complete symbol with the hasSymbol() method.
     */
    public Complex getCurrentSample()
    {
        /* Calculate interpolated current sample */
        mCurrentSample.setValues(getInphase(mSamplingPoint), getQuadrature(mSamplingPoint));
        return mCurrentSample;
    }

    /**
     * Interpolated sample that is 1/2 symbol away from (after) the current sample.
     *
     * Note: this method should only be invoked after testing for a complete symbol with the hasSymbol() method.
     */
    public Complex getMiddleSample()
    {
        float halfDetectedSamplesPerSymbol = mDetectedSamplesPerSymbol / 2.0f;

        //Interpolated sample that is half a symbol away from (occurred before) the current sample.
        mMiddleSample.setValues(getInphase(halfDetectedSamplesPerSymbol),
                                getQuadrature(halfDetectedSamplesPerSymbol));
        return mMiddleSample;
    }

    /**
     * Returns the interpolated inphase value for the specified offset
     * @param interpolation into the buffer to calculate the interpolated sample
     * @return inphase value of the interpolated sample
     */
    public float getInphase(float interpolation)
    {
        if(interpolation < 0.0f)
        {
            return mInterpolator.filter(mDelayLineInphase, mDelayLinePointer, 0.0f);
        }
        else if(interpolation < 1.0f)
        {
            return mInterpolator.filter(mDelayLineInphase, mDelayLinePointer, interpolation);
        }
        else
        {
            int offset = (int)FastMath.floor(interpolation);
            return mInterpolator.filter(mDelayLineInphase, mDelayLinePointer + offset, interpolation - offset);
        }
    }

    /**
     * Returns the interpolated quadrature value for the specified offset
     * @param interpolation into the buffer to calculate the interpolated sample
     * @return quadrature value of the interpolated sample
     */
    public float getQuadrature(float interpolation)
    {
        if(interpolation < 0.0f)
        {
            return mInterpolator.filter(mDelayLineQuadrature, mDelayLinePointer, 0.0f);
        }
        else if(interpolation < 1.0f)
        {
            return mInterpolator.filter(mDelayLineQuadrature, mDelayLinePointer, interpolation);
        }
        else
        {
            int offset = (int)FastMath.floor(interpolation);
            return mInterpolator.filter(mDelayLineQuadrature, mDelayLinePointer + offset, interpolation - offset);
        }
    }
}
