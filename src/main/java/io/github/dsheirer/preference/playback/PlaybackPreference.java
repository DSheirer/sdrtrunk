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

package io.github.dsheirer.preference.playback;

import io.github.dsheirer.gui.preference.playback.ToneFrequency;
import io.github.dsheirer.gui.preference.playback.ToneUtil;
import io.github.dsheirer.gui.preference.playback.ToneVolume;
import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.mixer.MixerChannelConfiguration;
import io.github.dsheirer.source.mixer.MixerManager;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User preferences for audio playback
 */
public class PlaybackPreference extends Preference
{
    private static final String PREFERENCE_KEY_USE_AUDIO_SEGMENT_DROP_TONE = "audio.playback.segment.drop.tone";
    private static final String PREFERENCE_KEY_DROP_TONE_FREQUENCY = "audio.playback.segment.drop.frequency";
    private static final String PREFERENCE_KEY_DROP_TONE_VOLUME = "audio.playback.segment.drop.volume";

    private static final String PREFERENCE_KEY_USE_AUDIO_SEGMENT_START_TONE = "audio.playback.segment.start.tone";
    private static final String PREFERENCE_KEY_START_TONE_FREQUENCY = "audio.playback.segment.start.frequency";
    private static final String PREFERENCE_KEY_START_TONE_VOLUME = "audio.playback.segment.start.volume";

    private static final String PREFERENCE_KEY_MIXER_CHANNEL_CONFIG = "audio.playback.mixer.channel.configuration";
    private static final int TONE_LENGTH_SAMPLES = 180;

    private final static Logger mLog = LoggerFactory.getLogger(PlaybackPreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(PlaybackPreference.class);
    private Boolean mUseAudioSegmentStartTone;
    private Boolean mUseAudioSegmentDropTone;
    private ToneFrequency mStartToneFrequency;
    private ToneVolume mStartToneVolume;
    private ToneFrequency mDropToneFrequency;
    private ToneVolume mDropToneVolume;
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
     * Indicates if an audio segment drop tone should be used.
     */
    public boolean getUseAudioSegmentDropTone()
    {
        if(mUseAudioSegmentDropTone == null)
        {
            mUseAudioSegmentDropTone = mPreferences.getBoolean(PREFERENCE_KEY_USE_AUDIO_SEGMENT_DROP_TONE, true);
        }

        return mUseAudioSegmentDropTone;
    }

    /**
     * Sets the preference for using an audio segment drop tone
     */
    public void setUseAudioSegmentDropTone(boolean use)
    {
        mUseAudioSegmentDropTone = use;
        mPreferences.putBoolean(PREFERENCE_KEY_USE_AUDIO_SEGMENT_DROP_TONE, use);
        notifyPreferenceUpdated();
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
     * Frequency for the drop tone
     */
    public ToneFrequency getDropToneFrequency()
    {
        if(mDropToneFrequency == null)
        {
            int frequency = mPreferences.getInt(PREFERENCE_KEY_DROP_TONE_FREQUENCY, ToneFrequency.F500.getValue());
            mDropToneFrequency = ToneFrequency.fromValue(frequency);
        }

        return mDropToneFrequency;
    }

    /**
     * Sets the frequency for the drop tone
     */
    public void setDropToneFrequency(ToneFrequency toneFrequency)
    {
        mDropToneFrequency = toneFrequency;
        mPreferences.putInt(PREFERENCE_KEY_DROP_TONE_FREQUENCY, toneFrequency.getValue());
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
     * Drop tone volume
     */
    public ToneVolume getDropToneVolume()
    {
        if(mDropToneVolume == null)
        {
            int volume = mPreferences.getInt(PREFERENCE_KEY_DROP_TONE_VOLUME, ToneVolume.V3.getValue());
            mDropToneVolume = ToneVolume.fromValue(volume);
        }

        return mDropToneVolume;
    }

    /**
     * Sets the drop tone volume
     */
    public void setDropToneVolume(ToneVolume toneVolume)
    {
        mDropToneVolume = toneVolume;
        mPreferences.putInt(PREFERENCE_KEY_DROP_TONE_VOLUME, toneVolume.getValue());
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
     * Buffer with samples for the audio segment drop tone
     */
    public float[] getDropTone()
    {
        if(getUseAudioSegmentDropTone())
        {
            return ToneUtil.getTone(getDropToneFrequency(), getDropToneVolume(), TONE_LENGTH_SAMPLES);
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

            if(defaultConfig != null)
            {
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
            else
            {
                mLog.error("Error - no system audio devices available");
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
