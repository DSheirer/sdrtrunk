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

import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audio gain that normalizes audio amplitude against an objective amplitude value where the audio amplitude values
 * normally fall in the range of -1.0 to 1.0.
 *
 * Removes DC bias on a per audio sample buffer basis.
 *
 * This control is designed to work on a per-call basis by monitoring the maximum observed amplitude and adjusting the
 * gain to normalize the amplitude toward the objective amplitude value.  This control is designed to be reset at the
 * beginning/end of each call segment so that the max observed amplitude is relevant to the current call segment.
 *
 * Min and max gains should be sized to constrain the applied value during periods of extended silence at the
 * beginning of an audio segment or where a single amplitude spike might erroneously affect gain across the segment.
 */
public class AudioGainAndDcFilter
{
    private static final Logger mLog = LoggerFactory.getLogger(AudioGainAndDcFilter.class);
    private static final float MAX_AMPLITUDE = 0.95f;
    private static final float GAIN_LOOP_BANDWIDTH = 0.0015f;
    private float mMinGain;
    private float mMaxGain;
    private float mCurrentGain;
    private float mObjectiveGain;
    private float mObjectiveAmplitude;
    private float mMaxObservedAmplitude;

    /**
     * Constructs an instance
     * @param minGain to apply to the incoming sample stream.
     * @param maxGain to apply
     * @param objectiveAmplitude to achieve by adjusting gain between min and max.
     */
    public AudioGainAndDcFilter(float minGain, float maxGain, float objectiveAmplitude)
    {
        mMinGain = minGain;
        mMaxGain = maxGain;
        mObjectiveAmplitude = objectiveAmplitude;
        reset();
    }

    /**
     * Resets this gain control to prepare for the next audio call/segment.
     */
    public void reset()
    {
        mMaxObservedAmplitude = 0.0f;
        mObjectiveGain = 1.0f;
        mCurrentGain = 1.0f;
    }

    /**
     * Process a buffer of audio samples and apply gain.
     * @param samples to adjust.
     * @return amplified audio samples
     */
    public float[] process(float[] samples)
    {
        float currentAmplitude;
        float dcAccumulator = 0.0f;

        //Decay the max observed value by 10% each buffer so that an initial spike doesn't carry across all buffers
        mMaxObservedAmplitude *= 0.9f;

        for(float sample: samples)
        {
            dcAccumulator += sample;
            currentAmplitude = FastMath.min(Math.abs(sample), 2.0f);

            if(currentAmplitude > mMaxObservedAmplitude)
            {
                mMaxObservedAmplitude = currentAmplitude;
            }
        }

        mObjectiveGain = mObjectiveAmplitude / mMaxObservedAmplitude;

        if(mObjectiveGain > mMaxGain)
        {
            mObjectiveGain = mMaxGain;
        }
        else if(mObjectiveGain < mMinGain)
        {
            mObjectiveGain = mMinGain;
        }

        float dcOffset = dcAccumulator / samples.length;
        float gain = mCurrentGain;
        float objective = mObjectiveGain;
        float[] processed = new float[samples.length];
        boolean objectiveAchieved = (gain == objective);
        float amplified;

        for(int x = 0; x < samples.length; x++)
        {
            if(!objectiveAchieved)
            {
                gain += ((objective - gain) * GAIN_LOOP_BANDWIDTH);
                if(Math.abs(objective - gain) < 0.00005f)
                {
                    gain = objective;
                    objectiveAchieved = true;
                }
            }

            amplified = (samples[x] - dcOffset) * gain;
            amplified = FastMath.min(amplified, MAX_AMPLITUDE);
            amplified = FastMath.max(amplified, -MAX_AMPLITUDE);
            processed[x] = amplified;
        }

        mCurrentGain = gain;
        mObjectiveGain = objective;
        return processed;
    }
}
