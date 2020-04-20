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
package io.github.dsheirer.dsp.fsk;

import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleBuffer
{
    private final static Logger mLog = LoggerFactory.getLogger(SampleBuffer.class);

    protected boolean[] mDelayLine;
    protected int mDelayLinePointer = 0;

    protected int mTwiceSamplesPerSymbol;
    private int mIntegerSamplesPerSymbol;
    private int mHalfIntegerSamplesPerSymbol;
    private int mPositiveSampleDecisionCount;
    protected int mSymbolStart;
    protected int mSymbolEnd;
    protected float mSamplesPerSymbol;
    protected float mMidSymbolSamplingPoint;
    protected float mSymbolTimingGain;

    /**
     * Buffer to store complex sample data and produce interpolated samples.
     * @param samplesPerSymbol
     */
    public SampleBuffer(float samplesPerSymbol, float symbolTimingGain)
    {
        mSamplesPerSymbol = samplesPerSymbol;
        mSymbolTimingGain = symbolTimingGain;

        mIntegerSamplesPerSymbol = (int)(samplesPerSymbol + 0.5f);
        mHalfIntegerSamplesPerSymbol = (int)(samplesPerSymbol / 2.0f + 0.5f);
        mMidSymbolSamplingPoint = samplesPerSymbol + mHalfIntegerSamplesPerSymbol;
        mTwiceSamplesPerSymbol = (int) FastMath.floor(2.0 * samplesPerSymbol);
        mDelayLine = new boolean[2 * mTwiceSamplesPerSymbol];
    }

    /**
     * Stores the sample in the sample buffer to support making symbol decisions.
     *
     * Note: use the hasSymbol() method to determine if sufficient samples have been collected to make a symbol
     * determination.
     *
     * @param sample to store in the sample buffer
     */
    public void receive(boolean sample)
    {
        mMidSymbolSamplingPoint--;

        mDelayLine[mDelayLinePointer] = sample;
        mDelayLine[mDelayLinePointer + mTwiceSamplesPerSymbol] = sample;

        //Increment pointer and keep pointer in bounds
        mDelayLinePointer++;
        mDelayLinePointer = mDelayLinePointer % mTwiceSamplesPerSymbol;
    }

    /**
     * Adjusts samples per symbol and symbol timing counters and increments the sample counter to collect another
     * symbol.
     *
     * @param symbolTimingError from a symbol timing error detector
     */
    public void resetAndAdjust(float symbolTimingError)
    {
        mMidSymbolSamplingPoint += (mSamplesPerSymbol + (symbolTimingError * mSymbolTimingGain));
    }

    public void setTimingGain(float gain)
    {
        mSymbolTimingGain = gain;
    }

    /**
     * Indicates if this buffer has accumulated enough samples to represent a full symbol
     */
    public boolean hasSymbol()
    {
        return mMidSymbolSamplingPoint < 1.0f;
    }

    /**
     * Calculates the symbol as a majority decision where the number of positive/true samples are greater than half
     * of the samples per symbol.  Symbol decision is based on the samples collected over the preceeding 0.5 to 1.5
     * symbol periods.
     *
     * @return a true symbol if the majority of the samples were true or positive, otherwise a false symbol.
     */
    public boolean getSymbol()
    {
        mPositiveSampleDecisionCount = 0;

        //Symbol samples are 0.5 and 1.5 symbols ahead of the current buffer pointer, but 0.5 to 1.5 symbol period
        //before the most recently stored sample decision.
        mSymbolStart = mDelayLinePointer + mHalfIntegerSamplesPerSymbol;
        mSymbolEnd = mSymbolStart + mIntegerSamplesPerSymbol;

        for(int x = mSymbolStart; x < mSymbolEnd; x++)
        {
            if(mDelayLine[x])
            {
                mPositiveSampleDecisionCount++;
            }
        }

        return mPositiveSampleDecisionCount > mHalfIntegerSamplesPerSymbol;
    }
}
