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

package io.github.dsheirer.preference.application;

import io.github.dsheirer.gui.theme.Theme;
import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General/Miscellaneous preferences
 */
public class ApplicationPreference extends Preference
{
    private static final String PREFERENCE_KEY_CHANNEL_AUTO_DIAGNOSTIC_MONITORING = "automatic.diagnostic.monitoring";
    private static final String PREFERENCE_KEY_CHANNEL_AUTO_START_TIMEOUT = "channel.auto.start.timeout";
    private static final String PREFERENCE_KEY_DARK_MODE = "dark.mode";
    private static final String PREFERENCE_KEY_THEME = "ui.theme";
    private static final String PREFERENCE_KEY_GUI_SCALE = "ui.gui.scale";

    /** Hard bounds for the GUI zoom factor. */
    public static final double MIN_GUI_SCALE = 0.5d;
    public static final double MAX_GUI_SCALE = 2.0d;
    public static final double DEFAULT_GUI_SCALE = 1.0d;

    private final static Logger mLog = LoggerFactory.getLogger(ApplicationPreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(ApplicationPreference.class);
    private Integer mChannelAutoStartTimeout;
    private Boolean mAutomaticDiagnosticMonitoring;
    private Theme mTheme;
    private Double mGuiScale;

    /**
     * Constructs an instance
     * @param updateListener to receive notifications that a preference has been updated
     */
    public ApplicationPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.APPLICATION;
    }


    /**
     * Channel auto-start timeout.  This is the countdown in seconds to allow the user to cancel the channel auto-start.
     * @return timeout in seconds.
     */
    public int getChannelAutoStartTimeout()
    {
        if(mChannelAutoStartTimeout == null)
        {
            mChannelAutoStartTimeout = mPreferences.getInt(PREFERENCE_KEY_CHANNEL_AUTO_START_TIMEOUT, 10);
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
        mPreferences.putInt(PREFERENCE_KEY_CHANNEL_AUTO_START_TIMEOUT, timeout);
        notifyPreferenceUpdated();
    }

    /**
     * Indicates if automatic diagnostic monitoring is enabled.
     * @return enabled.
     */
    public boolean isAutomaticDiagnosticMonitoring()
    {
        if(mAutomaticDiagnosticMonitoring == null)
        {
            mAutomaticDiagnosticMonitoring = mPreferences.getBoolean(PREFERENCE_KEY_CHANNEL_AUTO_DIAGNOSTIC_MONITORING, true);
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
        mPreferences.putBoolean(PREFERENCE_KEY_CHANNEL_AUTO_DIAGNOSTIC_MONITORING, enabled);
        notifyPreferenceUpdated();
    }

    /**
     * Returns the selected UI theme.  Falls back to a sensible default if no preference has been
     * stored - if the legacy {@code dark.mode} boolean is true the default is {@link Theme#DARK},
     * otherwise {@link Theme#LIGHT}.
     */
    public Theme getTheme()
    {
        if(mTheme == null)
        {
            String stored = mPreferences.get(PREFERENCE_KEY_THEME, null);
            if(stored != null)
            {
                mTheme = Theme.fromName(stored);
            }
            else
            {
                //Migrate from the older boolean preference so existing users keep their setting.
                mTheme = mPreferences.getBoolean(PREFERENCE_KEY_DARK_MODE, false) ? Theme.DARK : Theme.LIGHT;
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
        mPreferences.put(PREFERENCE_KEY_THEME, theme.name());
        //Keep the legacy boolean in sync so any older code path that reads it still behaves
        //correctly.
        mPreferences.putBoolean(PREFERENCE_KEY_DARK_MODE, theme.isDark());
        notifyPreferenceUpdated();
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
     * Returns the GUI zoom factor.  1.0 = 100% (default).  Bounded to
     * [{@link #MIN_GUI_SCALE}, {@link #MAX_GUI_SCALE}].
     */
    public double getGuiScale()
    {
        if(mGuiScale == null)
        {
            mGuiScale = mPreferences.getDouble(PREFERENCE_KEY_GUI_SCALE, DEFAULT_GUI_SCALE);
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
        mPreferences.putDouble(PREFERENCE_KEY_GUI_SCALE, clamped);
        notifyPreferenceUpdated();
    }

    private static double clampScale(double scale)
    {
        if(Double.isNaN(scale) || scale < MIN_GUI_SCALE) return MIN_GUI_SCALE;
        if(scale > MAX_GUI_SCALE) return MAX_GUI_SCALE;
        return scale;
    }
}
