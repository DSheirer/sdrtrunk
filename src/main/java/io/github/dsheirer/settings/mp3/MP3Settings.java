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

package io.github.dsheirer.settings.mp3;

import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.settings.AbstractSettings;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings for playlists
 */
public class MP3Settings extends AbstractSettings
{
    public static final boolean DEFAULT_NORMALIZE_BEFORE_ENCODE = false;
    private final static Logger mLog = LoggerFactory.getLogger(MP3Settings.class);
    private InputAudioFormat mInputAudioFormat;
    private MP3Setting mMP3Setting;

    private Boolean mNormalizeAudio;

    /**
     * Constructs this setting with an update listener
     * @param updateListener to receive notifications whenever these settings change
     */
    public MP3Settings(Listener<SettingsType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public SettingsType getSettingsType()
    {
        return SettingsType.MP3;
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
                String setting = Setting.MP3_SETTING.get(MP3Setting.getDefault().name());
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
     * Sets the MP3 setting setting
     */
    public void setMP3Setting(MP3Setting mp3Setting)
    {
        mMP3Setting = mp3Setting;
        Setting.MP3_SETTING.put(mp3Setting.name());
        notifySettingsUpdated();
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
                String rate = Setting.MP3_SAMPLE_RATE.get(InputAudioFormat.getDefault().name());
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

            //Ensure the input sample rate is supported by the MP3 setting, or change it to the first supported rate.
            MP3Setting setting = getMP3Setting();

            if(!setting.getSupportedSampleRates().contains(mInputAudioFormat))
            {
                InputAudioFormat supportedAudioFormat = InputAudioFormat.getDefault();

                //Default should always be supported by all mp3 settings, but fall-back to a supported rate if not.
                if(!setting.getSupportedSampleRates().contains(supportedAudioFormat))
                {
                    supportedAudioFormat = setting.getSupportedSampleRates().get(0);
                }

                mLog.warn("MP3 Setting [" + setting + "] does not support input sample rate [" + getAudioSampleRate().name() +
                        "] - updating to supported sample rate [" + supportedAudioFormat.name() + "]");
                setAudioSampleRate(supportedAudioFormat);
            }
        }

        return mInputAudioFormat;
    }

    /**
     * Sets the input Audio Sample Rate setting
     */
    public void setAudioSampleRate(InputAudioFormat inputAudioFormat)
    {
        mInputAudioFormat = inputAudioFormat;
        Setting.MP3_SAMPLE_RATE.put(mInputAudioFormat.name());
        notifySettingsUpdated();
    }

    /**
     * Indicates if the user prefers to normalize audio before encoding to MP3
     * @return true to normalize
     */
    public boolean isNormalizeAudioBeforeEncode()
    {
        if(mNormalizeAudio == null)
        {
            mNormalizeAudio = Setting.MP3_NORMALIZE_BEFORE_ENCODE.getBoolean(DEFAULT_NORMALIZE_BEFORE_ENCODE);
        }

        return mNormalizeAudio;
    }

    /**
     * Sets the preferenced to normalize audio before encoding to MP3.
     * @param normalizeAudio true to normalize
     */
    public void setNormalizeAudioBeforeEncode(boolean normalizeAudio)
    {
        mNormalizeAudio = normalizeAudio;
        Setting.MP3_NORMALIZE_BEFORE_ENCODE.putBoolean(normalizeAudio);
        notifySettingsUpdated();
    }
}
