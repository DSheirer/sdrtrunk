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

package io.github.dsheirer.preference.calibration;

import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;

/**
 * User preferences for CPU calibration for scalar vs vector SIMD implementations
 */
public class VectorCalibrationPreference extends Preference
{
    private static final String PREFERENCE_KEY_HIDE_CALIBRATION_DIALOG = "hide.calibration.dialog";
    private static final String PREFERENCE_KEY_VECTOR_ENABLED = "vector.enabled";

    private final static Logger mLog = LoggerFactory.getLogger(VectorCalibrationPreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(VectorCalibrationPreference.class);
    private Boolean mVectorEnabled;
    private Boolean mHideCalibrationDialog;

    /**
     * Constructs an instance
     * @param updateListener to receive notifications that a preference has been updated
     */
    public VectorCalibrationPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.CALIBRATION;
    }

    /**
     * Indicates if the calibration dialog should be shown.
     */
    public boolean isHideCalibrationDialog()
    {
        if(mHideCalibrationDialog == null)
        {
            mHideCalibrationDialog = mPreferences.getBoolean(PREFERENCE_KEY_HIDE_CALIBRATION_DIALOG, false);
        }

        return mHideCalibrationDialog;
    }

    /**
     * Sets the show calibration dialog
     */
    public void setHideCalibrationDialog(boolean hide)
    {
        mPreferences.putBoolean(PREFERENCE_KEY_HIDE_CALIBRATION_DIALOG, hide);
        mHideCalibrationDialog = hide;
        notifyPreferenceUpdated();
    }

    /**
     * Indicates if Vector (SIMD) operations are enabled.
     */
    public boolean isVectorEnabled()
    {
        if(mVectorEnabled == null)
        {
            mVectorEnabled = mPreferences.getBoolean(PREFERENCE_KEY_VECTOR_ENABLED, true);
        }

        return mVectorEnabled;
    }

    /**
     * Enables or disables Vector (SIMD) operations
     */
    public void setVectorEnabled(boolean enabled)
    {
        mPreferences.putBoolean(PREFERENCE_KEY_VECTOR_ENABLED, enabled);
        mVectorEnabled = enabled;
        notifyPreferenceUpdated();
    }
}
