/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nbfm;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import io.github.dsheirer.module.decode.dcs.DCSCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * DCS (Digital-Coded Squelch) detector for channel-level tone filtering.
 *
 * Adapted from SDRTrunk's DCSDecoder to work as an inline detector within
 * the NBFM decoder pipeline. Uses the same proven slope-based symbol detection
 * approach that reliably decodes 134.4 bps DCS from FM-demodulated audio.
 *
 * Designed for the decimated sample rate of the NBFM decoder pipeline.
 */
public class DCSDetector
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DCSDetector.class);

    private static final float DCS_BAUD_RATE = 134.4f;
    private static final int CODE_MASK = 0x7FFFFF;
    private static final int MAX_ONES_SEQUENCE = 6;
    private static final int CONFIRMATION_COUNT = 2;
    private static final int LOSS_CODEWORDS = 4;
    private static final int SLOPE_CALCULATION_PERIOD = 30;
    private static final double SLOPE_CALCULATION_SUM_XX = 2247.5;

    private final Set<DCSCode> mTargetCodes;
    private final float mSampleRate;
    private final float mBaudLength;
    private final float mSlopeThreshold;
    private final int mPostTransitionSkip;
    private final int mIdealTransitionMin;
    private final int mIdealTransitionMax;
    private final int mOverlap;

    private IRealFilter mLowPassFilter;

    private boolean mSymbol = false;
    private float mBaudCounter = 0f;
    private float mMaxSlope = 0;
    private float[] mResidual;
    private int mCode = 0;
    private int mExcessiveOneSequenceCounter = 0;
    private int mSamplesToSkip = 0;

    private DCSCode mDetectedCode = null;
    private int mConfirmationCounter = 0;
    private int mCodewordsSinceMatch = 0;

    private DCSDetectorListener mListener;

    public interface DCSDetectorListener
    {
        void dcsDetected(DCSCode code);
        void dcsLost();
    }

    public DCSDetector(Set<DCSCode> targetCodes, float sampleRate)
    {
        mTargetCodes = targetCodes;
        mSampleRate = sampleRate;
        mBaudLength = sampleRate / DCS_BAUD_RATE;

        float scale = sampleRate / 8000.0f;
        mSlopeThreshold = 0.002750f / scale;
        mPostTransitionSkip = (int)(30 * scale);
        mIdealTransitionMin = (int)(11 * scale);
        mIdealTransitionMax = (int)(19 * scale);
        mOverlap = (int)Math.ceil(mBaudLength);
        mResidual = new float[mOverlap];

        try
        {
            FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
                    .sampleRate((int)sampleRate)
                    .gridDensity(16)
                    .oddLength(true)
                    .passBandCutoff(200)
                    .passBandAmplitude(1.0)
                    .passBandRipple(0.01)
                    .stopBandStart(300)
                    .stopBandAmplitude(0.0)
                    .stopBandRipple(0.03)
                    .build();

            RemezFIRFilterDesigner designer = new RemezFIRFilterDesigner(specification);

            if(designer.isValid())
            {
                mLowPassFilter = FilterFactory.getRealFilter(designer.getImpulseResponse());
            }
            else
            {
                LOGGER.warn("DCS low-pass filter design failed - using fallback");
                mLowPassFilter = FilterFactory.getRealFilter(
                        FilterFactory.getLowPass(sampleRate, 200, 300, 60,
                                io.github.dsheirer.dsp.window.WindowType.HAMMING, true));
            }
        }
        catch(FilterDesignException e)
        {
            LOGGER.warn("DCS filter design exception - using fallback", e);
            mLowPassFilter = FilterFactory.getRealFilter(
                    FilterFactory.getLowPass(sampleRate, 200, 300, 60,
                            io.github.dsheirer.dsp.window.WindowType.HAMMING, true));
        }

        LOGGER.debug("DCSDetector initialized: sample rate={}, baud length={}", sampleRate, mBaudLength);
    }

    public void setListener(DCSDetectorListener listener)
    {
        mListener = listener;
    }

    public void process(float[] samples)
    {
        if(samples == null || samples.length == 0)
        {
            return;
        }

        float[] filtered = mLowPassFilter.filter(samples);
        float[] buffer = new float[filtered.length + mOverlap];
        int timingAdjust;

        try
        {
            System.arraycopy(mResidual, 0, buffer, 0, mResidual.length);
            System.arraycopy(filtered, 0, buffer, mResidual.length, filtered.length);

            if(filtered.length >= mOverlap)
            {
                System.arraycopy(filtered, filtered.length - mOverlap, mResidual, 0, mOverlap);
            }

            for(int bufferPointer = 0; bufferPointer < samples.length; bufferPointer++)
            {
                if(bufferPointer + SLOPE_CALCULATION_PERIOD >= buffer.length)
                {
                    break;
                }

                if(mSamplesToSkip > 0)
                {
                    mSamplesToSkip--;
                }
                else
                {
                    float slope = calculateSlope(buffer, bufferPointer);

                    if(mSymbol)
                    {
                        if(slope > mMaxSlope && mMaxSlope < -mSlopeThreshold)
                        {
                            mSymbol = false;

                            if(mBaudCounter < mIdealTransitionMin)
                            {
                                timingAdjust = 1;
                            }
                            else if(mBaudCounter > mIdealTransitionMax)
                            {
                                timingAdjust = -1;
                            }
                            else
                            {
                                timingAdjust = 0;
                            }

                            mBaudCounter += timingAdjust;
                            mSamplesToSkip = mPostTransitionSkip + timingAdjust;
                        }
                        else if(slope < mMaxSlope)
                        {
                            mMaxSlope = slope;
                        }
                    }
                    else
                    {
                        if(slope < mMaxSlope && mMaxSlope > mSlopeThreshold)
                        {
                            mSymbol = true;

                            if(mBaudCounter < mIdealTransitionMin)
                            {
                                timingAdjust = 1;
                            }
                            else if(mBaudCounter > mIdealTransitionMax)
                            {
                                timingAdjust = -1;
                            }
                            else
                            {
                                timingAdjust = 0;
                            }

                            mBaudCounter += timingAdjust;
                            mSamplesToSkip = mPostTransitionSkip + timingAdjust;
                        }
                        else if(slope > mMaxSlope)
                        {
                            mMaxSlope = slope;
                        }
                    }
                }

                mBaudCounter++;

                if(mBaudCounter > mBaudLength)
                {
                    mCode = (Integer.rotateLeft(mCode, 1) + (mSymbol ? 1 : 0)) & CODE_MASK;

                    if(DCSCode.hasValue(mCode))
                    {
                        handleDetection(DCSCode.fromValue(mCode));
                    }
                    else
                    {
                        handleNoDetection();
                    }

                    mBaudCounter -= mBaudLength;

                    if(mSymbol)
                    {
                        mExcessiveOneSequenceCounter++;
                        if(mExcessiveOneSequenceCounter > MAX_ONES_SEQUENCE)
                        {
                            mSymbol = false;
                            mMaxSlope = -1f;
                            mExcessiveOneSequenceCounter = 0;
                        }
                    }
                    else
                    {
                        mExcessiveOneSequenceCounter = 0;
                    }
                }
            }
        }
        catch(Exception e)
        {
            LOGGER.warn("Unexpected error while processing DCS samples", e);
        }
    }

    private float calculateSlope(float[] samples, int offset)
    {
        double xbar = 0;
        double ybar = samples[offset];
        double sumXY = 0;
        double fact1, dx, dy;

        for(int x = 1; x < SLOPE_CALCULATION_PERIOD; x++)
        {
            fact1 = 1.0 + x;
            dx = x - xbar;
            dy = samples[offset + x] - ybar;
            sumXY += dx * dy * (x / (1.0 + x));
            xbar += dx / fact1;
            ybar += dy / fact1;
        }

        return (float)(sumXY / SLOPE_CALCULATION_SUM_XX);
    }

    private void handleDetection(DCSCode code)
    {
        if(code == null || code == DCSCode.UNKNOWN)
        {
            return;
        }

        if(mTargetCodes != null && !mTargetCodes.isEmpty() && !mTargetCodes.contains(code))
        {
            return;
        }

        mCodewordsSinceMatch = 0;

        if(mDetectedCode == code)
        {
            if(mConfirmationCounter < CONFIRMATION_COUNT)
            {
                mConfirmationCounter++;
                if(mConfirmationCounter >= CONFIRMATION_COUNT && mListener != null)
                {
                    mListener.dcsDetected(code);
                }
            }
            else if(mListener != null)
            {
                mListener.dcsDetected(code);
            }
        }
        else
        {
            mDetectedCode = code;
            mConfirmationCounter = 1;
        }
    }

    private void handleNoDetection()
    {
        if(mDetectedCode != null)
        {
            mCodewordsSinceMatch++;
            if(mCodewordsSinceMatch >= (LOSS_CODEWORDS * 23))
            {
                mDetectedCode = null;
                mConfirmationCounter = 0;
                if(mListener != null)
                {
                    mListener.dcsLost();
                }
            }
        }
    }

    public void reset()
    {
        mSymbol = false;
        mBaudCounter = 0;
        mMaxSlope = 0;
        mCode = 0;
        mExcessiveOneSequenceCounter = 0;
        mSamplesToSkip = 0;
        mDetectedCode = null;
        mConfirmationCounter = 0;
        mCodewordsSinceMatch = 0;
        mResidual = new float[mOverlap];
    }

    public DCSCode getDetectedCode()
    {
        return (mConfirmationCounter >= CONFIRMATION_COUNT) ? mDetectedCode : null;
    }
}
