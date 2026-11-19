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

package io.github.dsheirer.module.decode.edacs.provoice;

import io.github.dsheirer.audio.AudioSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.audio.codec.mbe.ImbeAudioModule;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.audio.squelch.SquelchStateEvent;
import io.github.dsheirer.dsp.filter.equalizer.GraphicEqualizer;
import io.github.dsheirer.dsp.gain.NonClippingGain;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;

/**
 * Plays ProVoice voice frames through the JMBE IMBE codec.  ProVoice uses the IMBE vocoder;
 * the decoder hands over each voice frame re-packed as a P25 IMBE frame.
 */
public class ProVoiceAudioModule extends ImbeAudioModule
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ProVoiceAudioModule.class);
    private static final int SILENCE_SAMPLES = 160; //20 ms at 8 kHz
    private final NonClippingGain mGain = new NonClippingGain(5.0f, 0.95f);
    private final Listener<SquelchStateEvent> mSquelchStateListener = this::squelch;
    private volatile GraphicEqualizer mGraphicEQ;
    private long mFrameCount;

    /**
     * A segment whose loudest sample stays below this (full scale = 1.0) is silence.  Measured on Cambridge,
     * 2026-11-13: a transmission of 94 error-free frames with no sound peaked at 0.0012 (the codec's comfort
     * noise); the quietest of 20 transmissions with speech peaked at 0.31.
     */
    public static final float SILENCE_PEAK = 0.01f;
    private float mSegmentPeak;
    private int mSegmentFrames;
    private long mSilentSegmentCount;

    public ProVoiceAudioModule(UserPreferences userPreferences, AliasList aliasList)
    {
        super(userPreferences, aliasList);
    }

    /**
     * Graphic equalizer for the decoded audio, or null for none
     */
    public void setGraphicEqualizer(GraphicEqualizer equalizer)
    {
        mGraphicEQ = equalizer;
    }

    @Override
    public void receive(IMessage message)
    {
        if(message instanceof ProVoiceMessage voice && hasAudioCodec())
        {
            for(byte[] frame : voice.getIMBEFrames())
            {
                if(frame == null)
                {
                    addAudio(new float[SILENCE_SAMPLES]); //damaged frame, concealed as silence
                    mFrameCount++;
                    mSegmentFrames++;
                    continue;
                }

                float[] audio = getAudioCodec().getAudio(frame);

                //JMBE 2.x applies its own AGC - a fixed gain on top of it would clip
                if(!hasLibraryGainControl())
                {
                    audio = mGain.apply(audio);
                }

                GraphicEqualizer equalizer = mGraphicEQ;

                if(equalizer != null && equalizer.isEnabled())
                {
                    audio = equalizer.process(audio);
                }

                for(float sample : audio)
                {
                    float magnitude = Math.abs(sample);

                    if(magnitude > mSegmentPeak)
                    {
                        mSegmentPeak = magnitude;
                    }
                }

                addAudio(audio);
                mFrameCount++;
                mSegmentFrames++;
            }
        }
    }

    /**
     * A segment that is silence from start to finish is flagged so that it is not recorded or streamed.
     */
    @Override
    protected void segmentClosing(AudioSegment audioSegment)
    {
        if(mSegmentFrames > 0 && mSegmentPeak < SILENCE_PEAK)
        {
            audioSegment.setSilent(true);
            mSilentSegmentCount++;
            //DEBUG - per-call detail, written when its Diagnostics category is on
            LOGGER.debug(String.format("ProVoice transmission TO:%s FROM:%s was silence throughout (%.1f s, loudest " +
                    "sample %.4f of full scale) - not recorded, not streamed",
                audioSegment.getIdentifierCollection().getToIdentifier(),
                audioSegment.getIdentifierCollection().getFromIdentifier(), mSegmentFrames * 0.02, mSegmentPeak));
        }

        mSegmentPeak = 0;
        mSegmentFrames = 0;
    }

    /**
     * Number of audio segments flagged as silence
     */
    public long getSilentSegmentCount()
    {
        return mSilentSegmentCount;
    }

    /**
     * IMBE voice frames played (20 ms each)
     */
    public long getFrameCount()
    {
        return mFrameCount;
    }

    private void squelch(SquelchStateEvent event)
    {
        if(event.getSquelchState() == SquelchState.SQUELCH)
        {
            closeAudioSegment();
        }
    }

    @Override
    public Listener<SquelchStateEvent> getSquelchStateListener()
    {
        return mSquelchStateListener;
    }

    @Override
    public void reset()
    {
        getIdentifierCollection().clear();
    }

    @Override
    public void start()
    {
    }
}
