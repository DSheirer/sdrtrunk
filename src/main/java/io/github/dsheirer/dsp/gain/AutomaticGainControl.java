/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.dsp.gain;

import io.github.dsheirer.buffer.FloatCircularBuffer;
import io.github.dsheirer.source.wave.RealWaveSource;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Audio automatic gain control.
 */
public class AutomaticGainControl
{
    private static final Logger mLog = LoggerFactory.getLogger(AutomaticGainControl.class);
    private static final float SAMPLE_RATE = 8000;
    /* Signal delay line - time delay in seconds */
    private static final float DELAY_TIME_CONSTANT = 0.015f;
    /* Peak detector window - time delay in seconds */
    private static final float WINDOW_TIME_CONSTANT = 0.018f;
    /* Attack time constants in seconds */
    private static final float ATTACK_RISE_TIME_CONSTANT = 0.002f;
    private static final float ATTACK_FALL_TIME_CONSTANT = 0.005f;
    private static final float ATTACK_RISE_ALPHA = 1.0f - (float)FastMath.exp(-1.0 / SAMPLE_RATE * ATTACK_RISE_TIME_CONSTANT);
    private static final float ATTACK_FALL_ALPHA = 1.0f - (float)FastMath.exp(-1.0 / SAMPLE_RATE * ATTACK_FALL_TIME_CONSTANT);
    /* AGC decay value in milliseconds (20 to 5000) */
    private static final float DECAY = 200.0f;
    /* Ratio between rise and fall times of decay time constants - adjust for best action with SSB */
    private static final float DECAY_RISEFALL_RATIO = 0.3f;
    private static final float DECAY_RISE_ALPHA = 1.0f - (float)FastMath.exp(-1.0 / (SAMPLE_RATE * DECAY * .001 * DECAY_RISEFALL_RATIO));
    private static final float DECAY_FALL_ALPHA = 1.f - (float)FastMath.exp(-1.0 / (SAMPLE_RATE * DECAY * .001));
    /* Keep max input and output the same */
    private static final float MAX_AMPLITUDE = 1.0f; //1.0;
    private static final float MAX_MANUAL_AMPLITUDE = 1.0f; //1.0;
    /* Specifies AGC manual gain in dB if AGC is not active ( 0 to 100 dB) */
    private static final float MANUAL_GAIN = 0.0f;
    private static final float MANUAL_AGC_GAIN = MAX_MANUAL_AMPLITUDE * (float)FastMath.pow(10.0, MANUAL_GAIN / 20.0);
    /* Limit output to about 3db of maximum */
    private static final float AGC_OUT_SCALE = 0.95f; //0.7f;
    /* Specifies dB reduction in output at knee from max output level (0 - 10dB) */
    private static final float SLOPE_FACTOR = 2.0f;
    /* Specifies the threshold when the AGC kicks in (nominal range -160 to 0 dB) */
    private static final float THRESHOLD = -20.0f; //-20.0f
    private static final float KNEE = THRESHOLD / 20.0f; //20.0f
    private static final float GAIN_SLOPE = SLOPE_FACTOR / 100.0f;
    private static final float FIXED_GAIN = AGC_OUT_SCALE * (float)FastMath.pow(10.0, KNEE * (GAIN_SLOPE - 1.0));
    /* Constant for calc log() so that a value of 0 magnitude = -8 */
    private static final float MIN_CONSTANT = 3.2767E-4f;
    private boolean mAGCEnabled = true;
    private float mPeakMagnitude = -5.0f;
    private float mAttackAverage = 0.0f;
    private float mDecayAverage = 0.0f;
    private FloatCircularBuffer mDelayBuffer = new FloatCircularBuffer((int)(SAMPLE_RATE * DELAY_TIME_CONSTANT));
    private FloatCircularBuffer mMagnitudeBuffer = new FloatCircularBuffer((int)(SAMPLE_RATE * WINDOW_TIME_CONSTANT), mPeakMagnitude);
    private float mMaxGain = 0f;
    private float mMinGain = 100000f;

    /**
     * Construct an instance
     */
    public AutomaticGainControl()
    {
        System.out.println("Fixed gain: " + FIXED_GAIN + " Knee: " + KNEE + " Gain Slope:" + GAIN_SLOPE);
        System.out.println("Manual gain: " + MANUAL_AGC_GAIN);
    }

    public void reset()
    {
        mLog.info("Previous - Min Gain: " + mMinGain + " Max Gain: " + mMaxGain);
        mLog.info("Resetting - Peak:" + mPeakMagnitude + " Attack:" + mAttackAverage + " Decay:" + mDecayAverage + " Mag Max:" + mMagnitudeBuffer.max(mPeakMagnitude));
        mPeakMagnitude = -8.0f;
        mAttackAverage = -8.0f;
        mDecayAverage = -8.0f;
        mDelayBuffer.reset(0.0f);
        mMagnitudeBuffer.reset(mPeakMagnitude);
    }

    /**
     * Applies gain to the samples contained in the input buffer and returns a new buffer containing the
     * processed samples.
     *
     * @param input samples to process
     * @return output samples with gain applied
     */
    public float[] process(float[] input)
    {
        float[] output = new float[input.length];

        for(int x = 0; x < input.length; x++)
        {
            output[x] = process(input[x]);
        }

        return output;
    }

    /**
     * Processes the input sample and applies gain
     *
     * @param currentSample to process
     * @return gain applied sample
     */
    public float process(float currentSample)
    {
        float delayedSample = mDelayBuffer.get(currentSample);

        float gain = MANUAL_AGC_GAIN;

        if(mAGCEnabled)
        {
            float currentMagnitude = (float)(FastMath.log10(FastMath.abs(currentSample) + MIN_CONSTANT) - FastMath.log10(MAX_AMPLITUDE));
            float delayedMagnitude = mMagnitudeBuffer.get(currentMagnitude);

            if(currentMagnitude > mPeakMagnitude)
            {
                /* Use current magnitude as peak if it's larger */
                mPeakMagnitude = currentMagnitude;
            }
            else if(delayedMagnitude == mPeakMagnitude)
            {
                /* If delayed magnitude is the current peak, then find a new peak */
                mPeakMagnitude = mMagnitudeBuffer.max(-3.4845634f);
            }

            /* Exponential decay mode */
            if(mPeakMagnitude > mAttackAverage)
            {
                mAttackAverage = ((1.0f - ATTACK_RISE_ALPHA) * mAttackAverage) + (ATTACK_RISE_ALPHA * mPeakMagnitude);
            }
            else
            {
                mAttackAverage = ((1.0f - ATTACK_FALL_ALPHA) * mAttackAverage) + (ATTACK_FALL_ALPHA * mPeakMagnitude);
            }

            if(mPeakMagnitude > mDecayAverage)
            {
                mDecayAverage = ((1.0f - DECAY_RISE_ALPHA) * mDecayAverage) + (DECAY_RISE_ALPHA * mPeakMagnitude);
            }
            else
            {
                mDecayAverage = ((1.0f - DECAY_FALL_ALPHA) * mDecayAverage) + (DECAY_RISE_ALPHA * mPeakMagnitude);
            }


            float magnitude = FastMath.max(mAttackAverage, mDecayAverage);

            if(magnitude < KNEE)
            {
                gain = FIXED_GAIN;
            }
            else
            {
                gain = AGC_OUT_SCALE * (float)FastMath.pow(10.0, magnitude * (GAIN_SLOPE - 1.0));
            }

            System.out.println("Current Mag: " + currentMagnitude + " Delay Mag: " + delayedMagnitude +
                    " Peak:" + mPeakMagnitude + " Gain:" + gain +
                    " Attack Avg:" + mAttackAverage + " Decay Avg:" + mDecayAverage);
        }

        if(gain < mMinGain)
        {
            mMinGain = gain;
        }
        if(gain > mMaxGain)
        {
            mMaxGain = gain;
        }

        return delayedSample * gain;
    }

    /**
     * Enables or disables Automatic Gain Control (AGC).
     */
    public void setAGCEnabled(boolean enabled)
    {
        mAGCEnabled = enabled;
    }

    /**
     * Indicates if AGC is enabled
     *
     * @return true=AGC, false=MANUAL GAIN
     */
    public boolean isAGCEnabled()
    {
        return mAGCEnabled;
    }

}
