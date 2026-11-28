/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.settings.duplicate;

import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.audio.broadcast.PatchGroupStreamingOption;
import io.github.dsheirer.settings.AbstractSettings;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings for call management
 */
public class CallManagementSettings extends AbstractSettings implements ICallManagementProvider
{
    public static final boolean DEFAULT_DETECT_DUPLICATE_TALKGROUP = true;
    public static final boolean DEFAULT_DETECT_DUPLICATE_RADIO = false;
    public static final boolean DEFAULT_SUPPRESS_DUPLICATE_PLAYBACK = true;
    public static final boolean DEFAULT_SUPPRESS_DUPLICATE_RECORDING = true;
    public static final boolean DEFAULT_SUPPRESS_DUPLICATE_STREAMING = true;
    public static final PatchGroupStreamingOption DEFAULT_PATCH_GROUP_STREAMING = PatchGroupStreamingOption.PATCH_GROUP;

    private final static Logger mLog = LoggerFactory.getLogger(CallManagementSettings.class);
    private Boolean mDuplicateCallDetectionByTalkgroupEnabled;
    private Boolean mDuplicateCallDetectionByRadioEnabled;
    private Boolean mDuplicatePlaybackSuppressionEnabled;
    private Boolean mDuplicateRecordingSuppressionEnabled;
    private Boolean mDuplicateStreamingSuppressionEnabled;
    private PatchGroupStreamingOption mPatchGroupStreamingOption;

    /**
     * Constructs an instance
     * @param updateListener to receive notifications that a setting has been updated
     */
    public CallManagementSettings(Listener<SettingsType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public SettingsType getSettingsType()
    {
        return SettingsType.DUPLICATE_CALL_DETECTION;
    }

    /**
     * Indicates if duplicate call detection is enabled for either talkgroups or radio identifiers.
     */
    public boolean isDuplicateCallDetectionEnabled()
    {
        return isDuplicateCallDetectionByTalkgroupEnabled() || isDuplicateCallDetectionByRadioEnabled();
    }

    /**
     * Setting for detecting duplicate calls by talkgroup or patchgroup
     */
    public boolean isDuplicateCallDetectionByTalkgroupEnabled()
    {
        if(mDuplicateCallDetectionByTalkgroupEnabled == null)
        {
            mDuplicateCallDetectionByTalkgroupEnabled =
                Setting.CALL_DETECT_DUPLICATE_TALKGROUP.getBoolean(DEFAULT_DETECT_DUPLICATE_TALKGROUP);
        }

        return mDuplicateCallDetectionByTalkgroupEnabled;
    }

    /**
     * Sets the setting for detecting duplicate calls by talkgroup
     */
    public void setDuplicateCallDetectionByTalkgroupEnabled(boolean enabled)
    {
        Setting.CALL_DETECT_DUPLICATE_TALKGROUP.putBoolean(enabled);
        mDuplicateCallDetectionByTalkgroupEnabled = enabled;
        notifySettingsUpdated();
    }

    /**
     * Setting for detecting duplicate calls by radio identifier
     */
    public boolean isDuplicateCallDetectionByRadioEnabled()
    {
        if(mDuplicateCallDetectionByRadioEnabled == null)
        {
            mDuplicateCallDetectionByRadioEnabled = Setting.CALL_DETECT_DUPLICATE_RADIO.getBoolean(DEFAULT_DETECT_DUPLICATE_RADIO);
        }

        return mDuplicateCallDetectionByRadioEnabled;
    }

    /**
     * Sets the setting for detecting duplicate calls by radio identifier
     */
    public void setDuplicateCallDetectionByRadioEnabled(boolean enabled)
    {
        Setting.CALL_DETECT_DUPLICATE_RADIO.putBoolean(enabled);
        mDuplicateCallDetectionByRadioEnabled = enabled;
        notifySettingsUpdated();
    }

    /**
     * Setting for suppressig playback of duplicate calls.
     */
    public boolean isDuplicatePlaybackSuppressionEnabled()
    {
        if(mDuplicatePlaybackSuppressionEnabled == null)
        {
            mDuplicatePlaybackSuppressionEnabled = Setting.CALL_SUPPRESS_DUPLICATE_PLAYBACK.getBoolean(DEFAULT_SUPPRESS_DUPLICATE_PLAYBACK);
        }

        return mDuplicatePlaybackSuppressionEnabled;
    }

    /**
     * Sets the setting for suppressing playback of duplicate calls.
     */
    public void setDuplicatePlaybackSuppressionEnabled(boolean enabled)
    {
        Setting.CALL_SUPPRESS_DUPLICATE_PLAYBACK.putBoolean(enabled);
        mDuplicatePlaybackSuppressionEnabled = enabled;
        notifySettingsUpdated();
    }

    /**
     * Setting for suppressing recording of duplicate calls.
     * @deprecated duplicate calls are always recorded and are deleted after the call database duplicate and Do Not
     * Monitor retention time (#2742).  Kept so that the stored setting value remains readable.
     */
    @Deprecated
    public boolean isDuplicateRecordingSuppressionEnabled()
    {
        if(mDuplicateRecordingSuppressionEnabled == null)
        {
            mDuplicateRecordingSuppressionEnabled = Setting.CALL_SUPPRESS_DUPLICATE_RECORDING.getBoolean(DEFAULT_SUPPRESS_DUPLICATE_RECORDING);
        }

        return mDuplicateRecordingSuppressionEnabled;
    }

    /**
     * Sets the setting for suppressing recording of duplicate calls.
     * @deprecated see {@link #isDuplicateRecordingSuppressionEnabled()}
     */
    @Deprecated
    public void setDuplicateRecordingSuppressionEnabled(boolean enabled)
    {
        Setting.CALL_SUPPRESS_DUPLICATE_RECORDING.putBoolean(enabled);
        mDuplicateRecordingSuppressionEnabled = enabled;
        notifySettingsUpdated();
    }

    /**
     * Setting for suppressing streaming of duplicate calls
     */
    public boolean isDuplicateStreamingSuppressionEnabled()
    {
        if(mDuplicateStreamingSuppressionEnabled == null)
        {
            mDuplicateStreamingSuppressionEnabled = Setting.CALL_SUPPRESS_DUPLICATE_STREAMING.getBoolean(DEFAULT_SUPPRESS_DUPLICATE_STREAMING);
        }

        return mDuplicateStreamingSuppressionEnabled;
    }

    /**
     * Sets the setting for suppressing streaming of duplicate calls
     */
    public void setDuplicateStreamingSuppressionEnabled(boolean enabled)
    {
        Setting.CALL_SUPPRESS_DUPLICATE_STREAMING.putBoolean(enabled);
        mDuplicateStreamingSuppressionEnabled = enabled;
        notifySettingsUpdated();
    }

    /**
     * Patch group streaming option setting.
     * @return option.
     */
    public PatchGroupStreamingOption getPatchGroupStreamingOption()
    {
        if(mPatchGroupStreamingOption == null)
        {
            String option = Setting.CALL_PATCHGROUP_STREAMING.get(DEFAULT_PATCH_GROUP_STREAMING.name());

            if(option != null)
            {
                try
                {
                    mPatchGroupStreamingOption = PatchGroupStreamingOption.valueOf(option);
                }
                catch(Exception e)
                {
                    //Ignore ... we'll use the default value.
                }
            }

            if(mPatchGroupStreamingOption == null)
            {
                mPatchGroupStreamingOption = PatchGroupStreamingOption.PATCH_GROUP;
            }
        }

        return mPatchGroupStreamingOption;
    }

    /**
     * Sets the patch group streaming option setting.
     * @param patchGroupStreamingOption setting
     */
    public void setPatchGroupStreamingOption(PatchGroupStreamingOption patchGroupStreamingOption)
    {
        if(patchGroupStreamingOption != null)
        {
            Setting.CALL_PATCHGROUP_STREAMING.put(patchGroupStreamingOption.name());
            mPatchGroupStreamingOption = patchGroupStreamingOption;
            notifySettingsUpdated();
        }
    }
}
