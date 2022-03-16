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

package io.github.dsheirer.preference.mp3;

import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;

/**
 * User preferences for playlists
 */
public class MP3Preference extends Preference
{
    private static final String PREFERENCE_KEY_AUDIO_MP3_SETTING = "audio.mp3.setting";
    private static final String PREFERENCE_KEY_AUDIO_MP3_SAMPLE_RATE = "audio.mp3.sample.rate";
    private final static Logger mLog = LoggerFactory.getLogger(MP3Preference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(MP3Preference.class);
    private InputAudioFormat mInputAudioFormat;
    private MP3Setting mMP3Setting;

    /**
     * Constructs this preference with an update listener
     * @param updateListener to receive notifications whenever these preferences change
     */
    public MP3Preference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.MP3;
    }


    /**
     * MP3 Setting
     */
    public MP3Setting getMP3Setting()
    {
        if(mMP3Setting == null)
        {
            try
            {
                String setting = mPreferences.get(PREFERENCE_KEY_AUDIO_MP3_SETTING, MP3Setting.getDefault().name());
                mMP3Setting = MP3Setting.valueOf(setting);
            }
            catch(Exception e)
            {
                mLog.error("Error parsing mp3 setting preference", e);
            }

            if(mMP3Setting == null)
            {
                mMP3Setting = MP3Setting.getDefault();
            }
        }

        return mMP3Setting;
    }

    /**
     * Sets the MP3 setting preference
     */
    public void setMP3Setting(MP3Setting mp3Setting)
    {
        mMP3Setting = mp3Setting;
        mPreferences.put(PREFERENCE_KEY_AUDIO_MP3_SETTING, mp3Setting.name());
        notifyPreferenceUpdated();
    }

    /**
     * Preferred audio sample rate for input to the LAME MP3 encoder
     */
    public InputAudioFormat getAudioSampleRate()
    {
        if(mInputAudioFormat == null)
        {
            try
            {
                String rate = mPreferences.get(PREFERENCE_KEY_AUDIO_MP3_SAMPLE_RATE, InputAudioFormat.getDefault().name());
                mInputAudioFormat = InputAudioFormat.valueOf(rate);
            }
            catch(Exception e)
            {
                mLog.error("Error parsing mp3 setting preference", e);
            }

            if(mInputAudioFormat == null)
            {
                mInputAudioFormat = InputAudioFormat.getDefault();
            }
        }

        return mInputAudioFormat;
    }

    /**
     * Sets the input Audio Sample Rate preference
     */
    public void setAudioSampleRate(InputAudioFormat inputAudioFormat)
    {
        mInputAudioFormat = inputAudioFormat;
        mPreferences.put(PREFERENCE_KEY_AUDIO_MP3_SAMPLE_RATE, mInputAudioFormat.name());
        notifyPreferenceUpdated();
    }
}
