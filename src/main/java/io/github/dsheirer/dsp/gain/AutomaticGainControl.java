/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

import io.github.dsheirer.buffer.DoubleCircularBuffer;
import io.github.dsheirer.buffer.RealCircularBuffer;
import org.apache.commons.math3.util.FastMath;

public class AutomaticGainControl
{
    private static final double SAMPLE_RATE = 48000;

    /* Signal delay line - time delay in seconds */
//	private static final double DELAY_TIME_CONSTANT = 0.015;
    private static final double DELAY_TIME_CONSTANT = 0.01;

    /* Peak detector window - time delay in seconds */
    private static final double WINDOW_TIME_CONSTANT = 0.018;

    /* Attack time constants in seconds */
    private static final double ATTACK_RISE_TIME_CONSTANT = 0.002;
    private static final double ATTACK_FALL_TIME_CONSTANT = 0.005;

    private static final double ATTACK_RISE_ALPHA = 1.0 -
        FastMath.exp(-1.0 / SAMPLE_RATE * ATTACK_RISE_TIME_CONSTANT);

    private static final double ATTACK_FALL_ALPHA = 1.0 -
        FastMath.exp(-1.0 / SAMPLE_RATE * ATTACK_FALL_TIME_CONSTANT);

    /* AGC decay value in milliseconds (20 to 5000) */
    private static final double DECAY = 200;

    /* Ratio between rise and fall times of decay time constants - adjust for
     * best action with SSB */
    private static final double DECAY_RISEFALL_RATIO = 0.3;

    private static final double DECAY_RISE_ALPHA = 1.0 -
        FastMath.exp(-1.0 / (SAMPLE_RATE * DECAY * .001 * DECAY_RISEFALL_RATIO));

    private static final double DECAY_FALL_ALPHA = 1.0 -
        FastMath.exp(-1.0 / (SAMPLE_RATE * DECAY * .001));

    /* Hang timer release decay time constant in seconds */
    @SuppressWarnings("unused")
    private static final double RELEASE_TIME_CONSTANT = 0.05;

    /* Specifies the AGC Knee in dB if AGC is active (nominal range -160 to 0 dB) */
    private static final double THRESHOLD = -100;

    /* Limit output to about 3db of maximum */
    private static final double AGC_OUT_SCALE = 0.7;

    /* Keep max input and output the same */
    private static final double MAX_AMPLITUDE = 32767.0; //1.0;
    private static final double MAX_MANUAL_AMPLITUDE = 32767.0; //1.0;

    /* Specifies AGC manual gain in dB if AGC is not active ( 0 to 100 dB) */
    private static final double MANUAL_GAIN = 0.0;

    private static final double MANUAL_AGC_GAIN = MAX_MANUAL_AMPLITUDE *
        FastMath.pow(10.0, MANUAL_GAIN / 20.0);

    /* Specifies dB reduction in output at knee from max output level (0 - 10dB) */
    private static final double SLOPE_FACTOR = 2.0;

    private static final double KNEE = THRESHOLD / 20.0;

    private static final double GAIN_SLOPE = SLOPE_FACTOR / 100.0;

    private static final double FIXED_GAIN = AGC_OUT_SCALE *
        FastMath.pow(10.0, KNEE * (GAIN_SLOPE - 1.0));

    /* Constant for calc log() so that a value of 0 magnitude = -8 */
    private static final double MIN_CONSTANT = 3.2767E-4;

    private boolean mAGCEnabled = true;

    private double mPeakMagnitude = 0.0;
    private double mAttackAverage = 0.0;
    private double mDecayAverage = 0.0;

    private RealCircularBuffer mDelayBuffer = new RealCircularBuffer((int)(SAMPLE_RATE * DELAY_TIME_CONSTANT));
    private DoubleCircularBuffer mMagnitudeBuffer = new DoubleCircularBuffer((int)(SAMPLE_RATE * WINDOW_TIME_CONSTANT));

    public AutomaticGainControl()
    {
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
        float delayedSample = mDelayBuffer.putAndGet(currentSample);

        double gain = MANUAL_AGC_GAIN;

        if(mAGCEnabled)
        {
            double currentMagnitude = FastMath.log10(FastMath.abs(currentSample) + MIN_CONSTANT) - FastMath.log10(MAX_AMPLITUDE);

            double delayedMagnitude = mMagnitudeBuffer.get(currentMagnitude);

            if(currentMagnitude > mPeakMagnitude)
            {
                /* Use current magnitude as peak if it's larger */
                mPeakMagnitude = currentMagnitude;
            }
            else if(delayedMagnitude == mPeakMagnitude)
            {
                /* If delayed magnitude is the current peak, then find a new peak */
                mPeakMagnitude = mMagnitudeBuffer.max();
            }

            /* Exponential decay mode */
            if(mPeakMagnitude > mAttackAverage)
            {
                mAttackAverage = ((1.0 - ATTACK_RISE_ALPHA) * mAttackAverage) + (ATTACK_RISE_ALPHA * mPeakMagnitude);
            }
            else
            {
                mAttackAverage = ((1.0 - ATTACK_FALL_ALPHA) * mAttackAverage) + (ATTACK_FALL_ALPHA * mPeakMagnitude);
            }

            if(mPeakMagnitude > mDecayAverage)
            {
                mDecayAverage = ((1.0 - DECAY_RISE_ALPHA) * mDecayAverage) + (DECAY_RISE_ALPHA * mPeakMagnitude);
            }
            else
            {
                mDecayAverage = ((1.0 - DECAY_FALL_ALPHA) * mDecayAverage) + (DECAY_RISE_ALPHA * mPeakMagnitude);
            }

            double magnitude = FastMath.max(mAttackAverage, mDecayAverage);

            if(magnitude < KNEE)
            {
                gain = FIXED_GAIN;
            }
            else
            {
                gain = AGC_OUT_SCALE * FastMath.pow(10.0, magnitude * (GAIN_SLOPE - 1.0));
            }
        }

        return (float)(delayedSample * gain);
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
