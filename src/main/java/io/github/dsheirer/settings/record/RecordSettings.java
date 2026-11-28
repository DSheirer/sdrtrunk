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

package io.github.dsheirer.settings.record;

import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.AbstractSettings;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.record.BasebandRecordFormat;
import io.github.dsheirer.record.RecordFormat;
import io.github.dsheirer.record.RecordingFolderLayout;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Settings for playlists
 */
public class RecordSettings extends AbstractSettings
{
    public static final RecordFormat DEFAULT_RECORD_FORMAT = RecordFormat.MP3;
    public static final BasebandRecordFormat DEFAULT_BASEBAND_RECORD_FORMAT = BasebandRecordFormat.WAVE;
    public static final boolean DEFAULT_RECORD_ALL_CALLS = true;
    public static final RecordingFolderLayout DEFAULT_RECORDING_FOLDER_LAYOUT = RecordingFolderLayout.DEFAULT;
    public static final boolean DEFAULT_RECORDING_HOUR_FOLDER = false;
    private final static Logger mLog = LoggerFactory.getLogger(RecordSettings.class);
    private RecordFormat mAudioRecordFormat;
    private BasebandRecordFormat mBasebandRecordFormat;

    /**
     * Constructs this setting with an update listener
     * @param updateListener to receive notifications whenever these settings change
     */
    public RecordSettings(Listener<SettingsType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public SettingsType getSettingsType()
    {
        return SettingsType.RECORD;
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
                String format = Setting.RECORD_AUDIO_FORMAT.get(DEFAULT_RECORD_FORMAT.name());
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
        Setting.RECORD_AUDIO_FORMAT.put(audioRecordFormat.name());
        notifySettingsUpdated();
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
                String format = Setting.RECORD_BASEBAND_FORMAT.get(DEFAULT_BASEBAND_RECORD_FORMAT.name());
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
        Setting.RECORD_BASEBAND_FORMAT.put(basebandRecordFormat.name());
        notifySettingsUpdated();
    }

    /**
     * Indicates if the audio of every call is recorded.  When false, only calls with an alias that has the record
     * option are recorded.  Default: true.
     */
    public boolean isRecordAllCalls()
    {
        return Setting.RECORD_ALL_CALLS.getBoolean(DEFAULT_RECORD_ALL_CALLS);
    }

    /**
     * Sets whether the audio of every call is recorded
     */
    public void setRecordAllCalls(boolean recordAllCalls)
    {
        Setting.RECORD_ALL_CALLS.putBoolean(recordAllCalls);
        notifySettingsUpdated();
    }

    /**
     * How call audio recordings are organized into subfolders of the recordings folder.  Default: System / Date.
     */
    public RecordingFolderLayout getRecordingFolderLayout()
    {
        try
        {
            return RecordingFolderLayout.valueOf(Setting.RECORD_FOLDER_LAYOUT.get(
                DEFAULT_RECORDING_FOLDER_LAYOUT.name()));
        }
        catch(IllegalArgumentException iae)
        {
            return DEFAULT_RECORDING_FOLDER_LAYOUT;
        }
    }

    public void setRecordingFolderLayout(RecordingFolderLayout layout)
    {
        Setting.RECORD_FOLDER_LAYOUT.put(layout.name());
        notifySettingsUpdated();
    }

    /**
     * Indicates if an hour folder (00 - 23) is added below the date folder, for busy systems.  Default: false.
     */
    public boolean isRecordingHourFolder()
    {
        return Setting.RECORD_FOLDER_HOUR.getBoolean(DEFAULT_RECORDING_HOUR_FOLDER);
    }

    public void setRecordingHourFolder(boolean hourFolder)
    {
        Setting.RECORD_FOLDER_HOUR.putBoolean(hourFolder);
        notifySettingsUpdated();
    }
}
