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
package io.github.dsheirer.dsp.psk.pll;

import io.github.dsheirer.buffer.DoubleCircularBuffer;
import io.github.dsheirer.dsp.mixer.Oscillator;
import io.github.dsheirer.sample.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;

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
    public static final double TWO_PI = 2.0 * Math.PI;
    private DoubleCircularBuffer mErrorBuffer = new DoubleCircularBuffer(10);
    private double mDamping = Math.sqrt(2.0) / 2.0;
    private double mAlphaGain;
    private double mBetaGain;
    private double mLoopPhase = 0.0;
//    private double mLoopFrequency = 0.0;
    private double mLoopFrequency = Math.PI * 2.0 * -10.0 / 48000.0;
    private double mMaximumLoopFrequency;
    private Complex mCurrentVector = new Complex(0, 0);

    private Tracking mTracking = Tracking.SEARCHING;
    private ITrackingStateListener mTrackingStateListener;
    private boolean mAutomaticTrackingEnabled = true;

    /**
     * Costas Loop for tracking and correcting frequency error in a received carrier signal.
     *
     * @param sampleRate of the incoming samples
     * @param symbolRate of the digital signal
     */
    public CostasLoop(double sampleRate, double symbolRate)
    {
        mMaximumLoopFrequency = TWO_PI * (symbolRate / 2.0) / sampleRate;
        updateLoopBandwidth();
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

        if(mLoopFrequency > mMaximumLoopFrequency)
        {
            mLoopFrequency -= 2.0 * mMaximumLoopFrequency;
        }

        if(mLoopFrequency < -mMaximumLoopFrequency)
        {
            mLoopFrequency += 2.0 * mMaximumLoopFrequency;
        }
    }

    /**
     * Updates the loop bandwidth and alpha/beta gains according to the current loop synchronization state.
     */
    private void updateLoopBandwidth()
    {
        double bandwidth = TWO_PI / mTracking.getLoopBandwidth();

        mAlphaGain = (4.0 * mDamping * bandwidth) / (1.0 + (2.0 * mDamping * bandwidth) + (bandwidth * bandwidth));
        mBetaGain = (4.0 * bandwidth * bandwidth) / (1.0 + (2.0 * mDamping * bandwidth) + (bandwidth * bandwidth));
    }

    /**
     * Sets the tracking state for this costas loop.  The tracking state affects the aggressiveness of the alpha/beta
     * gain values in synchronizing with the signal carrier.
     *
     * @param tracking
     */
    public void setTracking(Tracking tracking)
    {
        mTracking = tracking;
        updateLoopBandwidth();

        mLog.debug("Tracking: " + tracking.name());

        if(mTrackingStateListener != null)
        {
            mTrackingStateListener.trackingStateChanged(tracking);
        }
    }

    /**
     * Sets the listener to receive notifications when the tracking state changes
     */
    public void setTrackingStateListener(ITrackingStateListener listener)
    {
        mTrackingStateListener = listener;
    }

    /**
     * Enables or disables automatic tracking state updates.  When enabled, the loop will automatically increase or
     * decrease the loop alpha/beta gain values based on the variance of the error tracking values submitted to the
     * loop control.  As the variance decreases, so will the gain values, and vice-versa.  Setting this to disabled
     * will force the tracker to retain the gain values associated with either the default or user-specified tracking
     * value.
     *
     * @param enabled true (default) to enable automatic gain tracking
     */
    public void setAutomaticTracking(boolean enabled)
    {
        mAutomaticTrackingEnabled = enabled;
    }

    /**
     * Increments the phase of the loop for each sample received at the sample rate.
     */
    public void increment()
    {
        mLoopPhase += mLoopFrequency;

        /* Keep the loop phase in bounds */
        normalizePhase();
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

    public double getMaximumLoopFrequency()
    {
        return mMaximumLoopFrequency;
    }

    /**
     * Updates the costas loop frequency and phase to adjust for the phase
     * error value
     *
     * @param phaseError - (-)= late and (+)= early
     */
    public void adjust(double phaseError)
    {
        mLoopFrequency += (mBetaGain * phaseError);
        mLoopPhase += mLoopFrequency + (mAlphaGain * phaseError);

        /* Maintain phase between +/- 2 * PI */
        normalizePhase();

        /* Limit frequency to +/- maximum loop frequency */
        limitFrequency();

//        checkLoopBandwidth();
    }

    private void checkLoopBandwidth()
    {
        mErrorBuffer.put(mLoopFrequency);

        double standardDeviation = mErrorBuffer.standardDeviation();

        switch(mTracking)
        {
            case SEARCHING:
                if(Tracking.COARSE.contains(standardDeviation))
                {
                    setTracking(Tracking.COARSE);
                }
                break;
            case COARSE:
                if(Tracking.COARSE.contains(standardDeviation))
                {
                    //Promote if possible - otherwise remain
                    if(Tracking.FINE.contains(standardDeviation))
                    {
                        setTracking(Tracking.FINE);
                    }
                }
                else
                {
                    setTracking(Tracking.SEARCHING);
                }
                break;
            case FINE:
                if(Tracking.FINE.contains(standardDeviation))
                {
                    //Promote if possible - otherwise remain
                    if(Tracking.LOCKED.contains(standardDeviation))
                    {
                        setTracking(Tracking.LOCKED);
                    }
                }
                else
                {
                    setTracking(Tracking.COARSE);
                }
                break;
            case LOCKED:
                if(!Tracking.LOCKED.contains(standardDeviation))
                {
                    setTracking(Tracking.FINE);
                }
                break;
        }
    }

    public double getErrorMean()
    {
        return mErrorBuffer.mean();
    }

    public double getErrorVariance()
    {
        return mErrorBuffer.variance();
    }

    /**
     * Normalizes the phase tracker to maintain within +/- 2 Pi
     */
    private void normalizePhase()
    {
        while(mLoopPhase > TWO_PI)
        {
            mLoopPhase -= TWO_PI;
        }

        while(mLoopPhase < -TWO_PI)
        {
            mLoopPhase += TWO_PI;
        }
    }

    /**
     * Constrains the frequency within the bounds of +/- loop frequency
     */
    private void limitFrequency()
    {
        if(mLoopFrequency > mMaximumLoopFrequency)
        {
            mLoopFrequency = mMaximumLoopFrequency;
        }

        if(mLoopFrequency < -mMaximumLoopFrequency)
        {
            mLoopFrequency = -mMaximumLoopFrequency;
        }
    }

    /**
     * Tracking state listener interface for receiving updates when the tracking state of the costas loop changes
     */
    public interface ITrackingStateListener
    {
        void trackingStateChanged(Tracking tracking);
    }

    public static void main(String[] arguments)
    {
        DecimalFormat decimalFormat = new DecimalFormat("0.000000");
        double sampleRate = 25000.0;
        int errorFrequency = 411;
        double loopBandwidth = 50.0;

        Oscillator oscillator = new Oscillator(errorFrequency, (int)sampleRate);
        CostasLoop costasLoop = new CostasLoop(sampleRate, 4800.0);
        costasLoop.setTrackingStateListener(new ITrackingStateListener()
        {
            @Override
            public void trackingStateChanged(Tracking tracking)
            {
                mLog.debug("Tracking State Changed: " + tracking.name());
            }
        });

        for(int x = 0; x < 1000; x++)
        {
            Complex complex = oscillator.getComplex();

            oscillator.rotate();
            costasLoop.increment();

            if(x % 5 == 0)
            {
                complex.multiply(costasLoop.getCurrentVector().conjugate());

                double phaseError = complex.angle();
                costasLoop.adjust(phaseError);

                double loopFrequency = costasLoop.getLoopFrequency() * sampleRate / TWO_PI;
                double maxFrequency = costasLoop.getMaximumLoopFrequency() * sampleRate / TWO_PI;

                mLog.debug(x + " Frequency: " + errorFrequency +
                    " Loop:" + decimalFormat.format(loopFrequency) +
                    " Max:" + maxFrequency +
                    " Detected Error:" + decimalFormat.format(phaseError) +
                    " Avg:" + decimalFormat.format(costasLoop.getErrorMean()) +
                    " Var:" + decimalFormat.format(costasLoop.getErrorVariance()));
            }
        }
    }

}
