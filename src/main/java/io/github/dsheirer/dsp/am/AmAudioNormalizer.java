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
package io.github.dsheirer.dsp.am;

/**
 * Converts an AM envelope to audio by removing the carrier (DC) and scaling the audio by the carrier level.
 *
 * An AM signal's envelope is carrier * (1 + modulation), so using the carrier as the gain reference gives audio that
 * follows the modulation depth of the transmission, independent of the received signal strength and without the
 * pumping of an audio peak based AGC.
 *
 * The carrier estimate uses a cumulative mean at the start of a transmission to lock quickly and without a transient,
 * and then transitions to a low-pass filter.  The DC removal follows the carrier estimate closely.  The gain reference
 * rises with the carrier but falls slowly so that when the carrier drops, the remaining noise isn't amplified to full
 * scale before the squelch closes.
 */
public class AmAudioNormalizer
{
    private static final double CARRIER_CUTOFF_HERTZ = 10.0;
    private static final double GAIN_DECAY_SECONDS = 2.0;

    /**
     * Gain reference limit, approximately -80 dBFS, that sets the maximum applied gain.
     */
    private static final float MINIMUM_CARRIER_LEVEL = 1E-4f;

    /**
     * Audio amplitude produced by 100% modulation.
     */
    private static final float FULL_MODULATION_AMPLITUDE = 0.75f;
    private static final float MAXIMUM_AMPLITUDE = 0.95f;

    private float mCarrierAlpha;
    private float mGainDecayAlpha;
    private int mSettlingSamples;
    private int mSampleCount;
    private float mCarrier;
    private float mGainReference;

    /**
     * Constructs an instance
     * @param sampleRate of the envelope samples in hertz
     */
    public AmAudioNormalizer(double sampleRate)
    {
        setSampleRate(sampleRate);
    }

    /**
     * Sets the sample rate and resets this normalizer.
     * @param sampleRate of the envelope samples in hertz
     */
    public void setSampleRate(double sampleRate)
    {
        mCarrierAlpha = (float)(1.0 - Math.exp(-2.0 * Math.PI * CARRIER_CUTOFF_HERTZ / sampleRate));
        mGainDecayAlpha = (float)(1.0 - Math.exp(-1.0 / (GAIN_DECAY_SECONDS * sampleRate)));
        mSettlingSamples = (int)Math.ceil(1.0 / mCarrierAlpha);
        reset();
    }

    /**
     * Resets this normalizer to prepare for a new transmission.
     */
    public void reset()
    {
        mSampleCount = 0;
        mCarrier = 0.0f;
        mGainReference = 0.0f;
    }

    /**
     * Converts the envelope sample to an audio sample.
     * @param envelope sample, unscaled
     * @return audio sample limited to the audio amplitude range.
     */
    public float process(float envelope)
    {
        if(mSampleCount < mSettlingSamples)
        {
            mSampleCount++;
            mCarrier += (envelope - mCarrier) / mSampleCount;
            mGainReference = mCarrier;
        }
        else
        {
            mCarrier += (envelope - mCarrier) * mCarrierAlpha;

            if(mCarrier > mGainReference)
            {
                mGainReference = mCarrier;
            }
            else
            {
                mGainReference += (mCarrier - mGainReference) * mGainDecayAlpha;
            }
        }

        float audio = (envelope - mCarrier) / Math.max(mGainReference, MINIMUM_CARRIER_LEVEL) * FULL_MODULATION_AMPLITUDE;
        return Math.clamp(audio, -MAXIMUM_AMPLITUDE, MAXIMUM_AMPLITUDE);
    }
}
