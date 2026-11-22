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

package io.github.dsheirer.dsp.am;

import io.github.dsheirer.dsp.magnitude.IMagnitudeCalculator;
import io.github.dsheirer.dsp.magnitude.MagnitudeFactory;
import io.github.dsheirer.dsp.squelch.AdaptiveSquelch;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceEvent;

/**
 * AM demodulator with integrated squelch control.
 */
public class SquelchingAMDemodulator implements ISquelchingDemodulator, Listener<SourceEvent>
{
    private static final float ZERO = 0.0f;
    private static final float ENVELOPE_GAIN = 1.0f;
    private static final double DEFAULT_SAMPLE_RATE = 50000.0;
    private static final double FADE_SECONDS = 0.005;

    /**
     * Audio delay, longer than the worst case 25 ms for the squelch to detect loss of signal plus the fade duration, so
     * that the audio fades out before the carrier drop reaches the audio normalizer, which would otherwise produce a
     * thump at the end of a transmission.
     */
    private static final double AUDIO_DELAY_SECONDS = 0.032;
    private final IMagnitudeCalculator mMagnitudeCalculator = MagnitudeFactory.getMagnitudeCalculator();
    private final IAmDemodulator mAmDemodulator;
    private final AdaptiveSquelch mAdaptiveSquelch;
    private final AmAudioNormalizer mAudioNormalizer = new AmAudioNormalizer(DEFAULT_SAMPLE_RATE);
    private float[] mDelayedEnvelope;
    private boolean[] mDelayedAudioEnabled;
    private int mDelayPointer = 0;
    private float mFadeGain = 0.0f;
    private float mFadeStep = (float)(1.0 / (DEFAULT_SAMPLE_RATE * FADE_SECONDS));
    private boolean mSquelchChanged = false;

    /**
     * Constructs an instance
     * @param squelchThreshold in decibels.  Signal power must exceed this threshold value for unsquelch.
     * @param squelchAutoTrack to enable the squelch noise floor auto tracking feature.
     * @param bandwidth of the channel in hertz
     */
    public SquelchingAMDemodulator(float squelchThreshold, boolean squelchAutoTrack, double bandwidth)
    {
        mAmDemodulator = AmDemodulatorFactory.getAmDemodulator(ENVELOPE_GAIN);
        mAdaptiveSquelch = new AdaptiveSquelch(squelchThreshold, squelchAutoTrack, bandwidth);
        setAudioDelay(DEFAULT_SAMPLE_RATE);
    }

    /**
     * Set or update the sample rate for the squelch to adjust the power level notification rate and for the audio
     * normalizer timing.
     * @param sampleRate in hertz
     */
    public void setSampleRate(int sampleRate)
    {
        mAdaptiveSquelch.setSampleRate(sampleRate);
        mAudioNormalizer.setSampleRate(sampleRate);
        mFadeStep = (float)Math.min(1.0, 1.0 / (sampleRate * FADE_SECONDS));
        setAudioDelay(sampleRate);
    }

    /**
     * Sizes the audio delay buffers.
     * @param sampleRate in hertz
     */
    private void setAudioDelay(double sampleRate)
    {
        int length = (int)Math.ceil(sampleRate * AUDIO_DELAY_SECONDS);
        mDelayedEnvelope = new float[length];
        mDelayedAudioEnabled = new boolean[length];
        mDelayPointer = 0;
    }

    /**
     * Registers the listener to receive notifications of squelch change events from the power squelch.
     */
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mAdaptiveSquelch.setSourceEventListener(listener);
    }

    /**
     * Demodulates the complex sample arrays.
     * @param i inphase sample array
     * @param q quadrature sample array
     * @return demodulated audio samples, normalized to the carrier level, or zero valued samples while squelched.
     */
    public float[] demodulate(float[] i, float[] q)
    {
        mAdaptiveSquelch.setSquelchChanged(false);
        setSquelchChanged(false);

        float[] magnitude = mMagnitudeCalculator.calculate(i, q);
        float[] demodulated = mAmDemodulator.demodulateMagnitude(magnitude);

        for(int x = 0; x < i.length; x++)
        {
            mAdaptiveSquelch.process(magnitude[x]);

            boolean audioEnabled = mAdaptiveSquelch.isAudioEnabled();
            float envelope = mDelayedEnvelope[mDelayPointer];
            boolean delayedAudioEnabled = mDelayedAudioEnabled[mDelayPointer];
            mDelayedEnvelope[mDelayPointer] = demodulated[x];
            mDelayedAudioEnabled[mDelayPointer] = audioEnabled;
            mDelayPointer = (mDelayPointer + 1) % mDelayedEnvelope.length;

            //Fade in once the delayed audio reaches the squelch open point and fade out as soon as the squelch detects
            //loss of signal, ahead of the delayed audio.
            mFadeGain = (audioEnabled && delayedAudioEnabled) ? Math.min(mFadeGain + mFadeStep, 1.0f) :
                    Math.max(mFadeGain - mFadeStep, 0.0f);

            if(mFadeGain > ZERO)
            {
                demodulated[x] = mAudioNormalizer.process(envelope) * mFadeGain;
            }
            else
            {
                demodulated[x] = ZERO;
                mAudioNormalizer.reset();
            }

            if(mAdaptiveSquelch.isSquelchChanged())
            {
                setSquelchChanged(true);
            }
        }

        return demodulated;
    }

    /**
     * Sets the threshold for squelch control
     * @param threshold (dB)
     */
    public void setSquelchThreshold(float threshold)
    {
        mAdaptiveSquelch.setSquelchThreshold(threshold);
    }

    @Override
    public void setSquelchAutoTrack(boolean autoTrack)
    {
        mAdaptiveSquelch.setSquelchAutoTrack(autoTrack);
    }

    /**
     * Indicates if the squelch state has changed during the processing of buffer(s)
     */
    public boolean isSquelchChanged()
    {
        return mSquelchChanged;
    }

    /**
     * Sets or resets the squelch changed flag.
     */
    private void setSquelchChanged(boolean changed)
    {
        mSquelchChanged = changed;
    }

    /**
     * Indicates if the squelch state is currently muted
     */
    public boolean isMuted()
    {
        return mAdaptiveSquelch.isMuted();
    }

    /**
     * Process source events initiated by the timer and end-user.
     * @param sourceEvent to process.
     */
    @Override
    public void receive(SourceEvent sourceEvent)
    {
        switch(sourceEvent.getEvent())
        {
            //Only forward squelch threshold & auto-track request events
            case REQUEST_CURRENT_SQUELCH_THRESHOLD:
            case REQUEST_CHANGE_SQUELCH_THRESHOLD:
            case REQUEST_CURRENT_SQUELCH_AUTO_TRACK:
            case REQUEST_CHANGE_SQUELCH_AUTO_TRACK:
            {
                mAdaptiveSquelch.receive(sourceEvent);
            }
        }
    }
}
