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

package io.github.dsheirer.settings.application;

import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.gui.theme.Theme;
import io.github.dsheirer.settings.AbstractSettings;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General/Miscellaneous settings
 */
public class ApplicationSettings extends AbstractSettings
{
    public static final int DEFAULT_CHANNEL_AUTO_START_TIMEOUT = 10;
    public static final Theme DEFAULT_THEME = Theme.LIGHT;
    public static final boolean DEFAULT_AUTOMATIC_DIAGNOSTIC_MONITORING = true;
    public static final boolean DEFAULT_COMPLEMENTARY_ALIAS_COLORS = false;

    /** Hard bounds for the GUI zoom factor. */
    public static final double MIN_GUI_SCALE = 0.5d;
    public static final double MAX_GUI_SCALE = 2.0d;
    public static final double DEFAULT_GUI_SCALE = 1.0d;

    private final static Logger mLog = LoggerFactory.getLogger(ApplicationSettings.class);
    private Integer mChannelAutoStartTimeout;
    private Boolean mAutomaticDiagnosticMonitoring;
    private Theme mTheme;
    private Double mGuiScale;
    private Boolean mComplementaryAliasColors;

    /**
     * Constructs an instance
     * @param updateListener to receive notifications that a setting has been updated
     */
    public ApplicationSettings(Listener<SettingsType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public SettingsType getSettingsType()
    {
        return SettingsType.APPLICATION;
    }


    /**
     * Channel auto-start timeout.  This is the countdown in seconds to allow the user to cancel the channel auto-start.
     * @return timeout in seconds.
     */
    public int getChannelAutoStartTimeout()
    {
        if(mChannelAutoStartTimeout == null)
        {
            mChannelAutoStartTimeout = Setting.APPLICATION_CHANNEL_AUTO_START_TIMEOUT.getInt(DEFAULT_CHANNEL_AUTO_START_TIMEOUT);
        }

        return mChannelAutoStartTimeout;
    }

    /**
     * Sets the channel auto-start timeout seconds value.
     * @param timeout in seconds.
     */
    public void setChannelAutoStartTimeout(int timeout)
    {
        mChannelAutoStartTimeout = timeout;
        Setting.APPLICATION_CHANNEL_AUTO_START_TIMEOUT.putInt(timeout);
        notifySettingsUpdated();
    }

    /**
     * Indicates if automatic diagnostic monitoring is enabled.
     * @return enabled.
     */
    public boolean isAutomaticDiagnosticMonitoring()
    {
        if(mAutomaticDiagnosticMonitoring == null)
        {
            mAutomaticDiagnosticMonitoring = Setting.APPLICATION_AUTOMATIC_DIAGNOSTIC_MONITORING.getBoolean(DEFAULT_AUTOMATIC_DIAGNOSTIC_MONITORING);
        }

        return mAutomaticDiagnosticMonitoring;
    }

    /**
     * Sets the enabled state for automatic diagnostic monitoring.
     * @param enabled true to turn on monitoring.
     */
    public void setAutomaticDiagnosticMonitoring(boolean enabled)
    {
        mAutomaticDiagnosticMonitoring = enabled;
        Setting.APPLICATION_AUTOMATIC_DIAGNOSTIC_MONITORING.putBoolean(enabled);
        notifySettingsUpdated();
    }

    /**
     * Returns the selected UI theme.  Falls back to a sensible default if no setting has been
     * stored - if the legacy {@code dark.mode} boolean is true the default is {@link Theme#DARK},
     * otherwise {@link Theme#LIGHT}.
     */
    public Theme getTheme()
    {
        if(mTheme == null)
        {
            String stored = Setting.APPLICATION_THEME.get(null);
            if(stored != null)
            {
                mTheme = Theme.fromName(stored);
            }
            else
            {
                //Migrate from the older boolean setting so existing users keep their setting.
                mTheme = Setting.APPLICATION_DARK_MODE.getBoolean(false) ? Theme.DARK : DEFAULT_THEME;
            }
        }

        return mTheme;
    }

    /**
     * Sets the selected UI theme.
     */
    public void setTheme(Theme theme)
    {
        if(theme == null)
        {
            theme = Theme.LIGHT;
        }

        mTheme = theme;
        Setting.APPLICATION_THEME.put(theme.name());
        //Keep the legacy boolean in sync so any older code path that reads it still behaves
        //correctly.
        Setting.APPLICATION_DARK_MODE.putBoolean(theme.isDark());
        notifySettingsUpdated();
    }

    /**
     * @return true if the currently selected theme is a dark palette.  Retained for callers that
     *         only care about the dark/light distinction.
     */
    public boolean isDarkMode()
    {
        return getTheme().isDark();
    }

    /**
     * Indicates if alias colors that lack contrast with the background are replaced with a complementary color when a
     * dark theme is active.
     * @return true if enabled.  Default is false, so that aliases display the color selected by the user.
     */
    public boolean isComplementaryAliasColors()
    {
        if(mComplementaryAliasColors == null)
        {
            mComplementaryAliasColors = Setting.APPLICATION_COMPLEMENTARY_ALIAS_COLORS.getBoolean(DEFAULT_COMPLEMENTARY_ALIAS_COLORS);
        }

        return mComplementaryAliasColors;
    }

    /**
     * Sets the enabled state for using complementary alias colors in dark themes.
     * @param enabled true to replace alias colors that lack contrast with the background.
     */
    public void setComplementaryAliasColors(boolean enabled)
    {
        mComplementaryAliasColors = enabled;
        Setting.APPLICATION_COMPLEMENTARY_ALIAS_COLORS.putBoolean(enabled);
        notifySettingsUpdated();
    }

    /**
     * Returns the GUI zoom factor.  1.0 = 100% (default).  Bounded to
     * [{@link #MIN_GUI_SCALE}, {@link #MAX_GUI_SCALE}].
     */
    public double getGuiScale()
    {
        if(mGuiScale == null)
        {
            mGuiScale = Setting.APPLICATION_GUI_SCALE.getDouble(DEFAULT_GUI_SCALE);
            mGuiScale = clampScale(mGuiScale);
        }

        return mGuiScale;
    }

    /**
     * Sets the GUI zoom factor.  Out-of-range values are clamped to the supported bounds.
     */
    public void setGuiScale(double scale)
    {
        double clamped = clampScale(scale);
        mGuiScale = clamped;
        Setting.APPLICATION_GUI_SCALE.putDouble(clamped);
        notifySettingsUpdated();
    }

    private static double clampScale(double scale)
    {
        if(Double.isNaN(scale) || scale < MIN_GUI_SCALE) return MIN_GUI_SCALE;
        if(scale > MAX_GUI_SCALE) return MAX_GUI_SCALE;
        return scale;
    }
}
