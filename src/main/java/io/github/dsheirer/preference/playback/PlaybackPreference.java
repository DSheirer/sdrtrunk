/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.preference.playback;

import io.github.dsheirer.gui.preference.playback.ToneFrequency;
import io.github.dsheirer.gui.preference.playback.ToneUtil;
import io.github.dsheirer.gui.preference.playback.ToneVolume;
import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.mixer.MixerChannel;
import io.github.dsheirer.source.mixer.MixerChannelConfiguration;
import io.github.dsheirer.source.mixer.MixerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;

/**
 * User preferences for audio playback
 */
public class PlaybackPreference extends Preference
{
    private static final String PREFERENCE_KEY_USE_AUDIO_SEGMENT_PREEMPT_TONE = "audio.playback.segment.preempt.tone";
    private static final String PREFERENCE_KEY_USE_AUDIO_SEGMENT_START_TONE = "audio.playback.segment.start.tone";
    private static final String PREFERENCE_KEY_START_TONE_FREQUENCY = "audio.playback.segment.start.frequency";
    private static final String PREFERENCE_KEY_START_TONE_VOLUME = "audio.playback.segment.start.volume";
    private static final String PREFERENCE_KEY_PREEMPT_TONE_FREQUENCY = "audio.playback.segment.preempt.frequency";
    private static final String PREFERENCE_KEY_PREEMPT_TONE_VOLUME = "audio.playback.segment.preempt.volume";
    private static final String PREFERENCE_KEY_MIXER_CHANNEL_CONFIG = "audio.playback.mixer.channel.configuration";
    private static final int TONE_LENGTH_SAMPLES = 180;

    private final static Logger mLog = LoggerFactory.getLogger(PlaybackPreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(PlaybackPreference.class);
    private Boolean mUseAudioSegmentStartTone;
    private Boolean mUseAudioSegmentPreemptTone;
    private ToneFrequency mStartToneFrequency;
    private ToneVolume mStartToneVolume;
    private ToneFrequency mPreemptToneFrequency;
    private ToneVolume mPreemptToneVolume;
    private MixerChannelConfiguration mMixerChannelConfiguration;

    /**
     * Constructs this preference with an update listener
     * @param updateListener to receive notifications whenever these preferences change
     */
    public PlaybackPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.PLAYBACK;
    }

    /**
     * Indicates if an audio segment start tone should be used.
     */
    public boolean getUseAudioSegmentStartTone()
    {
        if(mUseAudioSegmentStartTone == null)
        {
            mUseAudioSegmentStartTone = mPreferences.getBoolean(PREFERENCE_KEY_USE_AUDIO_SEGMENT_START_TONE, true);
        }

        return mUseAudioSegmentStartTone;
    }

    /**
     * Sets the preference for using an audio segment start tone
     */
    public void setUseAudioSegmentStartTone(boolean use)
    {
        mUseAudioSegmentStartTone = use;
        mPreferences.putBoolean(PREFERENCE_KEY_USE_AUDIO_SEGMENT_START_TONE, use);
        notifyPreferenceUpdated();
    }

    /**
     * Indicates if an audio segment preempt tone should be used.
     */
    public boolean getUseAudioSegmentPreemptTone()
    {
        if(mUseAudioSegmentPreemptTone == null)
        {
            mUseAudioSegmentPreemptTone = mPreferences.getBoolean(PREFERENCE_KEY_USE_AUDIO_SEGMENT_PREEMPT_TONE, true);
        }

        return mUseAudioSegmentPreemptTone;
    }

    /**
     * Sets the preference for using an audio segment preempt tone
     */
    public void setUseAudioSegmentPreemptTone(boolean use)
    {
        mUseAudioSegmentPreemptTone = use;
        mPreferences.putBoolean(PREFERENCE_KEY_USE_AUDIO_SEGMENT_PREEMPT_TONE, use);
        notifyPreferenceUpdated();
    }

    /**
     * Frequency for the start tone
     */
    public ToneFrequency getStartToneFrequency()
    {
        if(mStartToneFrequency == null)
        {
            int frequency = mPreferences.getInt(PREFERENCE_KEY_START_TONE_FREQUENCY, ToneFrequency.F700.getValue());
            mStartToneFrequency = ToneFrequency.fromValue(frequency);
        }

        return mStartToneFrequency;
    }

    /**
     * Sets the frequency for the start tone
     */
    public void setStartToneFrequency(ToneFrequency toneFrequency)
    {
        mStartToneFrequency = toneFrequency;
        mPreferences.putInt(PREFERENCE_KEY_START_TONE_FREQUENCY, toneFrequency.getValue());
        notifyPreferenceUpdated();
    }

    /**
     * Frequency for the preempt tone
     */
    public ToneFrequency getPreemptToneFrequency()
    {
        if(mPreemptToneFrequency == null)
        {
            int frequency = mPreferences.getInt(PREFERENCE_KEY_PREEMPT_TONE_FREQUENCY, ToneFrequency.F400.getValue());
            mPreemptToneFrequency = ToneFrequency.fromValue(frequency);
        }

        return mPreemptToneFrequency;
    }

    /**
     * Sets the frequency for the preempt tone
     */
    public void setPreemptToneFrequency(ToneFrequency toneFrequency)
    {
        mPreemptToneFrequency = toneFrequency;
        mPreferences.putInt(PREFERENCE_KEY_PREEMPT_TONE_FREQUENCY, toneFrequency.getValue());
        notifyPreferenceUpdated();
    }

    /**
     * Start tone volume
     */
    public ToneVolume getStartToneVolume()
    {
        if(mStartToneVolume == null)
        {
            int volume = mPreferences.getInt(PREFERENCE_KEY_START_TONE_VOLUME, ToneVolume.V3.getValue());
            mStartToneVolume = ToneVolume.fromValue(volume);
        }

        return mStartToneVolume;
    }

    /**
     * Sets the start tone volume
     */
    public void setStartToneVolume(ToneVolume toneVolume)
    {
        mStartToneVolume = toneVolume;
        mPreferences.putInt(PREFERENCE_KEY_START_TONE_VOLUME, toneVolume.getValue());
        notifyPreferenceUpdated();
    }

    /**
     * Preempt tone volume
     */
    public ToneVolume getPreemptToneVolume()
    {
        if(mPreemptToneVolume == null)
        {
            int volume = mPreferences.getInt(PREFERENCE_KEY_PREEMPT_TONE_VOLUME, ToneVolume.V5.getValue());
            mPreemptToneVolume = ToneVolume.fromValue(volume);
        }

        return mPreemptToneVolume;
    }

    /**
     * Sets the preempt tone volume
     */
    public void setPreemptToneVolume(ToneVolume toneVolume)
    {
        mPreemptToneVolume = toneVolume;
        mPreferences.putInt(PREFERENCE_KEY_PREEMPT_TONE_VOLUME, toneVolume.getValue());
        notifyPreferenceUpdated();
    }

    /**
     * Buffer with samples for the audio segment start tone
     */
    public float[] getStartTone()
    {
        if(getUseAudioSegmentStartTone())
        {
            return ToneUtil.getTone(getStartToneFrequency(), getStartToneVolume(), TONE_LENGTH_SAMPLES);
        }

        return null;
    }

    /**
     * Buffer with samples for the audio segment preempt tone
     */
    public float[] getPreemptTone()
    {
        if(getUseAudioSegmentPreemptTone())
        {
            return ToneUtil.getTone(getPreemptToneFrequency(), getPreemptToneVolume(), TONE_LENGTH_SAMPLES);
        }

        return null;
    }

    /**
     * Test tone to use for testing the currently selected mixer output
     */
    public float[] getMixerTestTone()
    {
        return ToneUtil.getTone(ToneFrequency.F1200, ToneVolume.V10, 800);
    }

    /**
     * Gets the preferred output mixer to use
     */
    public MixerChannelConfiguration getMixerChannelConfiguration()
    {
        if(mMixerChannelConfiguration == null)
        {
            MixerChannelConfiguration defaultConfig = MixerManager.getDefaultOutputMixer();

            String configName = mPreferences.get(PREFERENCE_KEY_MIXER_CHANNEL_CONFIG, defaultConfig.toString());

            for(MixerChannelConfiguration config: MixerManager.getOutputMixers())
            {
                if(config.toString().contentEquals(configName))
                {
                    mMixerChannelConfiguration = config;
                }
            }

            if(mMixerChannelConfiguration == null)
            {
                mMixerChannelConfiguration = defaultConfig;
            }
        }

        return mMixerChannelConfiguration;
    }

    /**
     * Sets the preferred output mixer to use
     */
    public void setMixerChannelConfiguration(MixerChannelConfiguration configuration)
    {
        mMixerChannelConfiguration = configuration;
        mPreferences.put(PREFERENCE_KEY_MIXER_CHANNEL_CONFIG, configuration.toString());
        notifyPreferenceUpdated();
    }
}
