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

package io.github.dsheirer.settings.calibration;

import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.AbstractSettings;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Settings for CPU calibration for scalar vs vector SIMD implementations
 */
public class VectorCalibrationSettings extends AbstractSettings
{
    public static final boolean DEFAULT_HIDE_CALIBRATION_DIALOG = false;
    public static final boolean DEFAULT_VECTOR_ENABLED = true;

    private final static Logger mLog = LoggerFactory.getLogger(VectorCalibrationSettings.class);
    private Boolean mVectorEnabled;
    private Boolean mHideCalibrationDialog;

    /**
     * Constructs an instance
     * @param updateListener to receive notifications that a setting has been updated
     */
    public VectorCalibrationSettings(Listener<SettingsType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public SettingsType getSettingsType()
    {
        return SettingsType.CALIBRATION;
    }

    /**
     * Indicates if the calibration dialog should be shown.
     */
    public boolean isHideCalibrationDialog()
    {
        if(mHideCalibrationDialog == null)
        {
            mHideCalibrationDialog = Setting.VECTOR_HIDE_CALIBRATION_DIALOG.getBoolean(DEFAULT_HIDE_CALIBRATION_DIALOG);
        }

        return mHideCalibrationDialog;
    }

    /**
     * Sets the show calibration dialog
     */
    public void setHideCalibrationDialog(boolean hide)
    {
        Setting.VECTOR_HIDE_CALIBRATION_DIALOG.putBoolean(hide);
        mHideCalibrationDialog = hide;
        notifySettingsUpdated();
    }

    /**
     * Indicates if Vector (SIMD) operations are enabled.
     */
    public boolean isVectorEnabled()
    {
        if(mVectorEnabled == null)
        {
            mVectorEnabled = Setting.VECTOR_ENABLED.getBoolean(DEFAULT_VECTOR_ENABLED);
        }

        return mVectorEnabled;
    }

    /**
     * Enables or disables Vector (SIMD) operations
     */
    public void setVectorEnabled(boolean enabled)
    {
        Setting.VECTOR_ENABLED.putBoolean(enabled);
        mVectorEnabled = enabled;
        notifySettingsUpdated();
    }
}
