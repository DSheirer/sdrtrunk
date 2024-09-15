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

package io.github.dsheirer.preference.duplicate;

import io.github.dsheirer.audio.broadcast.PatchGroupStreamingOption;
import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User preferences for call management
 */
public class CallManagementPreference extends Preference implements ICallManagementProvider
{
    private static final String PREFERENCE_KEY_DETECT_DUPLICATE_TALKGROUP = "duplicate.call.detect.talkgroup";
    private static final String PREFERENCE_KEY_DETECT_DUPLICATE_RADIO = "duplicate.call.detect.radio";
    private static final String PREFERENCE_KEY_SUPPRESS_DUPLICATE_PLAYBACK = "suppress.duplicate.audio.playback";
    private static final String PREFERENCE_KEY_SUPPRESS_DUPLICATE_RECORDING = "suppress.duplicate.audio.recording";
    private static final String PREFERENCE_KEY_SUPPRESS_DUPLICATE_STREAMING = "suppress.duplicate.audio.streaming";
    private static final String PREFERENCE_KEY_PATCHGROUP_STREAMING = "patchgroup.streaming";

    private final static Logger mLog = LoggerFactory.getLogger(CallManagementPreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(CallManagementPreference.class);
    private Boolean mDuplicateCallDetectionByTalkgroupEnabled;
    private Boolean mDuplicateCallDetectionByRadioEnabled;
    private Boolean mDuplicatePlaybackSuppressionEnabled;
    private Boolean mDuplicateRecordingSuppressionEnabled;
    private Boolean mDuplicateStreamingSuppressionEnabled;
    private PatchGroupStreamingOption mPatchGroupStreamingOption;

    /**
     * Constructs an instance
     * @param updateListener to receive notifications that a preference has been updated
     */
    public CallManagementPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.DUPLICATE_CALL_DETECTION;
    }

    /**
     * Indicates if duplicate call detection is enabled for either talkgroups or radio identifiers.
     */
    public boolean isDuplicateCallDetectionEnabled()
    {
        return isDuplicateCallDetectionByTalkgroupEnabled() || isDuplicateCallDetectionByRadioEnabled();
    }

    /**
     * Preference for detecting duplicate calls by talkgroup or patchgroup
     */
    public boolean isDuplicateCallDetectionByTalkgroupEnabled()
    {
        if(mDuplicateCallDetectionByTalkgroupEnabled == null)
        {
            mDuplicateCallDetectionByTalkgroupEnabled =
                mPreferences.getBoolean(PREFERENCE_KEY_DETECT_DUPLICATE_TALKGROUP, true);
        }

        return mDuplicateCallDetectionByTalkgroupEnabled;
    }

    /**
     * Sets the preference for detecting duplicate calls by talkgroup
     */
    public void setDuplicateCallDetectionByTalkgroupEnabled(boolean enabled)
    {
        mPreferences.putBoolean(PREFERENCE_KEY_DETECT_DUPLICATE_TALKGROUP, enabled);
        mDuplicateCallDetectionByTalkgroupEnabled = enabled;
        notifyPreferenceUpdated();
    }

    /**
     * Preference for detecting duplicate calls by radio identifier
     */
    public boolean isDuplicateCallDetectionByRadioEnabled()
    {
        if(mDuplicateCallDetectionByRadioEnabled == null)
        {
            mDuplicateCallDetectionByRadioEnabled = mPreferences.getBoolean(PREFERENCE_KEY_DETECT_DUPLICATE_RADIO, false);
        }

        return mDuplicateCallDetectionByRadioEnabled;
    }

    /**
     * Sets the preference for detecting duplicate calls by radio identifier
     */
    public void setDuplicateCallDetectionByRadioEnabled(boolean enabled)
    {
        mPreferences.putBoolean(PREFERENCE_KEY_DETECT_DUPLICATE_RADIO, enabled);
        mDuplicateCallDetectionByRadioEnabled = enabled;
        notifyPreferenceUpdated();
    }

    /**
     * Preference for suppressig playback of duplicate calls.
     */
    public boolean isDuplicatePlaybackSuppressionEnabled()
    {
        if(mDuplicatePlaybackSuppressionEnabled == null)
        {
            mDuplicatePlaybackSuppressionEnabled = mPreferences.getBoolean(PREFERENCE_KEY_SUPPRESS_DUPLICATE_PLAYBACK, true);
        }

        return mDuplicatePlaybackSuppressionEnabled;
    }

    /**
     * Sets the preference for suppressing playback of duplicate calls.
     */
    public void setDuplicatePlaybackSuppressionEnabled(boolean enabled)
    {
        mPreferences.putBoolean(PREFERENCE_KEY_SUPPRESS_DUPLICATE_PLAYBACK, enabled);
        mDuplicatePlaybackSuppressionEnabled = enabled;
        notifyPreferenceUpdated();
    }

    /**
     * Preference for suppressing recording of duplicate calls.
     */
    public boolean isDuplicateRecordingSuppressionEnabled()
    {
        if(mDuplicateRecordingSuppressionEnabled == null)
        {
            mDuplicateRecordingSuppressionEnabled = mPreferences.getBoolean(PREFERENCE_KEY_SUPPRESS_DUPLICATE_RECORDING, true);
        }

        return mDuplicateRecordingSuppressionEnabled;
    }

    /**
     * Sets the preference for suppressing recording of duplicate calls.
     */
    public void setDuplicateRecordingSuppressionEnabled(boolean enabled)
    {
        mPreferences.putBoolean(PREFERENCE_KEY_SUPPRESS_DUPLICATE_RECORDING, enabled);
        mDuplicateRecordingSuppressionEnabled = enabled;
        notifyPreferenceUpdated();
    }

    /**
     * Preference for suppressing streaming of duplicate calls
     */
    public boolean isDuplicateStreamingSuppressionEnabled()
    {
        if(mDuplicateStreamingSuppressionEnabled == null)
        {
            mDuplicateStreamingSuppressionEnabled = mPreferences.getBoolean(PREFERENCE_KEY_SUPPRESS_DUPLICATE_STREAMING, true);
        }

        return mDuplicateStreamingSuppressionEnabled;
    }

    /**
     * Sets the preference for suppressing streaming of duplicate calls
     */
    public void setDuplicateStreamingSuppressionEnabled(boolean enabled)
    {
        mPreferences.putBoolean(PREFERENCE_KEY_SUPPRESS_DUPLICATE_STREAMING, enabled);
        mDuplicateStreamingSuppressionEnabled = enabled;
        notifyPreferenceUpdated();
    }

    /**
     * Patch group streaming option preference.
     * @return option.
     */
    public PatchGroupStreamingOption getPatchGroupStreamingOption()
    {
        if(mPatchGroupStreamingOption == null)
        {
            String option = mPreferences.get(PREFERENCE_KEY_PATCHGROUP_STREAMING, PatchGroupStreamingOption.PATCH_GROUP.name());

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
     * Sets the patch group streaming option preference.
     * @param patchGroupStreamingOption preference
     */
    public void setPatchGroupStreamingOption(PatchGroupStreamingOption patchGroupStreamingOption)
    {
        if(patchGroupStreamingOption != null)
        {
            mPreferences.put(PREFERENCE_KEY_PATCHGROUP_STREAMING, patchGroupStreamingOption.name());
            mPatchGroupStreamingOption = patchGroupStreamingOption;
            notifyPreferenceUpdated();
        }
    }
}
