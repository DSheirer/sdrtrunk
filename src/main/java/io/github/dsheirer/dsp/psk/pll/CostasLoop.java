/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.dsp.psk.pll;

import io.github.dsheirer.sample.complex.Complex;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Costas Loop - phase locked loop designed to automatically synchronize to the incoming carrier frequency in order to
 * zeroize any frequency offset inherent in the signal due to either mis-tuning or carrier frequency drift.
 *
 * Costas loop structure was modeled from KA1RBI's OP25/cqpsk.py
 *
 * Loop bandwidth and alpha/beta gain formulas derived from:
 * http://www.trondeau.com/blog/2011/8/13/control-loop-gain-values.html
 */
public class CostasLoop implements IPhaseLockedLoop
{
    private final static Logger mLog = LoggerFactory.getLogger(CostasLoop.class);

    public static final double TWO_PI = 2.0 * FastMath.PI;

    private Complex mCurrentVector = new Complex(0, 0);
    private double mLoopPhase = 0.0;
    private double mLoopFrequency = 0.0;
    private double mMaximumLoopFrequency;
    private double mDamping = FastMath.sqrt(2.0) / 2.0;
    private double mAlphaGain;
    private double mBetaGain;
    private PLLBandwidth mPLLBandwidth = PLLBandwidth.BW_400;
    private double mSymbolRate;
    private double mSampleRate;
    private IFrequencyErrorProcessor mFrequencyErrorProcessor;
    private long mFrequencyError;
    private int mBaudCounter;

    /**
     * Costas Loop for tracking and correcting frequency error in a received carrier signal.
     *
     * @param sampleRate of the incoming samples
     * @param symbolRate of the digital signal
     */
    public CostasLoop(double sampleRate, double symbolRate)
    {
        mSymbolRate = symbolRate;
        mSampleRate = sampleRate;
        mMaximumLoopFrequency = TWO_PI * (symbolRate / 2.0) / sampleRate;
        updateLoopBandwidth();
    }

    /**
     * Registers the listener to receive the measured frequency error as tracked by the PLL once per second
     *
     * @param listener to receive frequency error measurement.
     */
    public void setFrequencyErrorProcessor(IFrequencyErrorProcessor listener)
    {
        mFrequencyErrorProcessor = listener;
    }

    /**
     * Corrects the current phase tracker when an inverted output is detected.  The inversion will take the form of
     * +/- 90 degrees or +/- 180 degrees, although the latter correction is equal regardless of the sign.
     *
     * If the supplied correction value places the loop frequency outside of the max frequency, then the frequency will
     * be corrected 360 degrees in the opposite direction to maintain within the max frequency bounds.
     *
     * @param correction as measured in radians
     */
    public void correctInversion(double correction)
    {
        mLoopFrequency += correction;

        while(mLoopFrequency > mMaximumLoopFrequency)
        {
            mLoopFrequency -= 2.0 * mMaximumLoopFrequency;
        }

        while(mLoopFrequency < -mMaximumLoopFrequency)
        {
            mLoopFrequency += 2.0 * mMaximumLoopFrequency;
        }
    }

    /**
     * Updates the loop bandwidth and alpha/beta gains according to the current loop synchronization state.
     */
    private void updateLoopBandwidth()
    {
        double bandwidth = TWO_PI / mPLLBandwidth.getLoopBandwidth();

        mAlphaGain = (4.0 * mDamping * bandwidth) / (1.0 + (2.0 * mDamping * bandwidth) + (bandwidth * bandwidth));
        mBetaGain = (4.0 * bandwidth * bandwidth) / (1.0 + (2.0 * mDamping * bandwidth) + (bandwidth * bandwidth));
    }

    /**
     * Sets the PLLBandwidth state for this costas loop.  The bandwidth affects the aggressiveness of the alpha/beta
     * gain values in synchronizing with the signal carrier.
     *
     * @param pllBandwidth
     */
    public void setPLLBandwidth(PLLBandwidth pllBandwidth)
    {
        if(mPLLBandwidth != pllBandwidth)
        {
            mPLLBandwidth = pllBandwidth;
            updateLoopBandwidth();
        }
    }

    /**
     * Increments the phase of the loop for each sample received at the sample rate.
     */
    public void increment()
    {
        mLoopPhase += mLoopFrequency;

        /* Normalize phase between +/- 2 * PI */
        if(mLoopPhase > TWO_PI)
        {
            mLoopPhase -= TWO_PI;
        }

        if(mLoopPhase < -TWO_PI)
        {
            mLoopPhase += TWO_PI;
        }
    }

    /**
     * Current vector of the loop.  Note: this value is updated for the current angle in radians each time this method
     * is invoked.
     */
    public Complex getCurrentVector()
    {
        mCurrentVector.setAngle(mLoopPhase);
        return mCurrentVector;
    }

    @Override
    public Complex incrementAndGetCurrentVector()
    {
        increment();
        return getCurrentVector();
    }

    public double getLoopFrequency()
    {
        return mLoopFrequency;
    }

    /**
     * Updates the costas loop frequency and phase to adjust for the phase error value
     *
     * @param phaseError - (-)= late and (+)= early
     */
    public void adjust(double phaseError)
    {
        mLoopFrequency += (mBetaGain * phaseError);
        mLoopPhase += mLoopFrequency + (mAlphaGain * phaseError);

        //Normalize phase between +/- 2 * PI
        if(mLoopPhase > TWO_PI)
        {
            mLoopPhase -= TWO_PI;
        }

        if(mLoopPhase < -TWO_PI)
        {
            mLoopPhase += TWO_PI;
        }

        //Limit frequency to +/- maximum loop frequency
        if(mLoopFrequency > mMaximumLoopFrequency)
        {
            mLoopFrequency = mMaximumLoopFrequency;
        }

        if(mLoopFrequency < -mMaximumLoopFrequency)
        {
            mLoopFrequency = -mMaximumLoopFrequency;
        }

        //Broadcast current frequency error measurement once a second to an external listener
        mBaudCounter++;

        if(mBaudCounter > mSymbolRate)
        {
            mBaudCounter = 0;

            mFrequencyError = (long)(mSampleRate / TWO_PI * mLoopFrequency);

            if(mFrequencyErrorProcessor != null)
            {
                mFrequencyErrorProcessor.processFrequencyError(mFrequencyError);
            }
        }
    }

    /**
     * Resets the PLL internal tracking values
     */
    @Override
    public void reset()
    {
        mLoopPhase = 0.0;
        mLoopFrequency = 0.0;
    }
}
