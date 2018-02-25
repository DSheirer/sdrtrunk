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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterpolatingSymbolBuffer2
{
    private static final float MAXIMUM_SAMPLES_PER_SYMBOL_DEVIATION = 0.02f; //2%
    private final static Logger mLog = LoggerFactory.getLogger(InterpolatingSymbolBuffer2.class);
    private Complex mPreceedingSample = new Complex(0,0);
    private Complex mCurrentSample = new Complex(0,0);
    private Complex mFollowingSample = new Complex(0,0);

    private float[] mDelayLineInphase;
    private float[] mDelayLineQuadrature;
    private float mSamplingPoint;
    private float mSampleCounterGain = 0.05f;
    private float mDetectedSamplesPerSymbol;
    private float mDetectedSamplesPerSymbolGain = 0.1f * mSampleCounterGain * mSampleCounterGain;
    private float mMaximumSamplesPerSymbol;;
    private float mMinimumSamplesPerSymbol;;

    private int mDelayLinePointer = 0;
    private int mTwiceSamplesPerSymbol;
    private RealInterpolator mInterpolator = new RealInterpolator(1.0f);
    private SymbolDecisionData2 mSymbolDecisionData2;

    public InterpolatingSymbolBuffer2(float samplesPerSymbol)
    {
        mSamplingPoint = samplesPerSymbol;
        mDetectedSamplesPerSymbol = samplesPerSymbol;
        mMaximumSamplesPerSymbol = samplesPerSymbol * (1.0f + MAXIMUM_SAMPLES_PER_SYMBOL_DEVIATION);
        mMinimumSamplesPerSymbol = samplesPerSymbol * (1.0f - MAXIMUM_SAMPLES_PER_SYMBOL_DEVIATION);
        mTwiceSamplesPerSymbol = (int)Math.floor(2.0 * samplesPerSymbol);
        mDelayLineInphase = new float[2 * mTwiceSamplesPerSymbol];
        mDelayLineQuadrature = new float[2 * mTwiceSamplesPerSymbol];

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
        mDetectedSamplesPerSymbol = mDetectedSamplesPerSymbol + (symbolTimingError * mDetectedSamplesPerSymbolGain);
        mDetectedSamplesPerSymbol = constrain(mDetectedSamplesPerSymbol, mMinimumSamplesPerSymbol, mMaximumSamplesPerSymbol);

        mLog.debug("Detected Samples Per Symbol: " + mDetectedSamplesPerSymbol);
        //Add another symbol's worth of samples to the counter and adjust timing based on gardner error
        increaseSampleCounter((mDetectedSamplesPerSymbol + (mSampleCounterGain * symbolTimingError)));
//        increaseSampleCounter(mDetectedSamplesPerSymbol);
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
     * Samples that precedes the current sample (uninterpolated).
     */
    public Complex getPreceedingSample()
    {
        mPreceedingSample.setValues(mDelayLineInphase[mDelayLinePointer + 3], mDelayLineQuadrature[mDelayLinePointer + 3]);

        return mPreceedingSample;
    }

    /**
     * Interpolated middle sample for the symbol.  Note: this method should only be invoked after testing for a
     * complete symbol with the hasSymbol() method.
     */
    public Complex getCurrentSample()
    {
        /* Calculate interpolated current sample */
        mCurrentSample.setValues(getInphase(mSamplingPoint), getQuadrature(mSamplingPoint));

        return mCurrentSample;
    }

    /**
     * Samples that follows the current sample (uninterpolated).
     */
    public Complex getFollowingSample()
    {
        mFollowingSample.setValues(mDelayLineInphase[mDelayLinePointer + 4], mDelayLineQuadrature[mDelayLinePointer + 4]);

        return mFollowingSample;
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

    /**
     * Constrains the value within the minimum-maximum range
     *
     * @param value to constrain
     * @param minimum value
     * @param maximum value
     * @return constrained value
     */
    public static float constrain(float value, float minimum, float maximum)
    {
        if(value > maximum)
        {
            return maximum;
        }

        if(value < minimum)
        {
            return minimum;
        }

        return value;
    }
}
