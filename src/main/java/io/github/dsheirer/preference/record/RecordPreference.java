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

package io.github.dsheirer.preference.record;

import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.record.BasebandRecordFormat;
import io.github.dsheirer.record.RecordFormat;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;

/**
 * User preferences for playlists
 */
public class RecordPreference extends Preference
{
    private static final String PREFERENCE_KEY_AUDIO_RECORD_FORMAT = "audio.record.format";
    private static final RecordFormat DEFAULT_RECORD_FORMAT = RecordFormat.MP3;
    private static final String PREFERENCE_KEY_BASEBAND_RECORD_FORMAT = "baseband.record.format";
    private static final BasebandRecordFormat DEFAULT_BASEBAND_RECORD_FORMAT = BasebandRecordFormat.WAVE;
    private final static Logger mLog = LoggerFactory.getLogger(RecordPreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(RecordPreference.class);
    private RecordFormat mAudioRecordFormat;
    private BasebandRecordFormat mBasebandRecordFormat;

    /**
     * Constructs this preference with an update listener
     * @param updateListener to receive notifications whenever these preferences change
     */
    public RecordPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.RECORD;
    }


    /**
     * Audio recording format
     */
    public RecordFormat getAudioRecordFormat()
    {
        if(mAudioRecordFormat == null)
        {
            try
            {
                String format = mPreferences.get(PREFERENCE_KEY_AUDIO_RECORD_FORMAT, DEFAULT_RECORD_FORMAT.name());
                mAudioRecordFormat = RecordFormat.valueOf(format);
            }
            catch(Exception e)
            {
                mLog.error("Error parsing record format preference", e);
            }

            if(mAudioRecordFormat == null)
            {
                mAudioRecordFormat = DEFAULT_RECORD_FORMAT;
            }
        }

        return mAudioRecordFormat;
    }

    /**
     * Sets the audio recording format
     */
    public void setAudioRecordFormat(RecordFormat audioRecordFormat)
    {
        mAudioRecordFormat = audioRecordFormat;
        mPreferences.put(PREFERENCE_KEY_AUDIO_RECORD_FORMAT, audioRecordFormat.name());
        notifyPreferenceUpdated();
    }

    /**
     * Baseband (complex I/Q) recording file format for tuner and channel baseband recordings.
     */
    public BasebandRecordFormat getBasebandRecordFormat()
    {
        if(mBasebandRecordFormat == null)
        {
            try
            {
                String format = mPreferences.get(PREFERENCE_KEY_BASEBAND_RECORD_FORMAT,
                    DEFAULT_BASEBAND_RECORD_FORMAT.name());
                mBasebandRecordFormat = BasebandRecordFormat.valueOf(format);
            }
            catch(Exception e)
            {
                mLog.error("Error parsing baseband record format preference", e);
            }

            if(mBasebandRecordFormat == null)
            {
                mBasebandRecordFormat = DEFAULT_BASEBAND_RECORD_FORMAT;
            }
        }

        return mBasebandRecordFormat;
    }

    /**
     * Sets the baseband (complex I/Q) recording file format
     */
    public void setBasebandRecordFormat(BasebandRecordFormat basebandRecordFormat)
    {
        mBasebandRecordFormat = basebandRecordFormat;
        mPreferences.put(PREFERENCE_KEY_BASEBAND_RECORD_FORMAT, basebandRecordFormat.name());
        notifyPreferenceUpdated();
    }
}
