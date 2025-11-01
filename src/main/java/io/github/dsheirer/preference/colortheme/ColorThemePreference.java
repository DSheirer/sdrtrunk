/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.preference.colortheme;

import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Color theme preferences for the application UI
 */
public class ColorThemePreference extends Preference
{
    private static final String PREFERENCE_KEY_DARK_MODE_ENABLED = "color.theme.dark.mode.enabled";
    private static final String PREFERENCE_KEY_USER_SET = "color.theme.user.set";
    
    private final static Logger mLog = LoggerFactory.getLogger(ColorThemePreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(ColorThemePreference.class);
    private Boolean mDarkModeEnabled;

    /**
     * Constructs an instance
     * @param updateListener to receive notifications that a preference has been updated
     */
    public ColorThemePreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.COLOR_THEME;
    }

    /**
     * Indicates if dark mode is enabled.
     * On first run, automatically detects Windows system theme.
     * @return true if dark mode is enabled.
     */
    public boolean isDarkModeEnabled()
    {
        if(mDarkModeEnabled == null)
        {
            // Check if user has explicitly set the preference
            boolean userHasSet = mPreferences.getBoolean(PREFERENCE_KEY_USER_SET, false);
            
            System.out.println("[ColorTheme] User has set preference: " + userHasSet);
            
            if(!userHasSet)
            {
                // First time - detect system theme and use as default
                System.out.println("[ColorTheme] First run detected - checking system theme...");
                boolean systemDarkMode = isSystemInDarkMode();
                mDarkModeEnabled = systemDarkMode;
                // Save the detected value to preferences
                mPreferences.putBoolean(PREFERENCE_KEY_DARK_MODE_ENABLED, systemDarkMode);
                System.out.println("[ColorTheme] Detected and saved: " + (systemDarkMode ? "DARK MODE" : "LIGHT MODE"));
                mLog.info("First run - detected Windows system theme: " + (systemDarkMode ? "Dark" : "Light") + " - applying as default");
            }
            else
            {
                // User has set preference before - use saved value
                mDarkModeEnabled = mPreferences.getBoolean(PREFERENCE_KEY_DARK_MODE_ENABLED, false);
                System.out.println("[ColorTheme] Using saved preference: " + (mDarkModeEnabled ? "DARK MODE" : "LIGHT MODE"));
            }
        }

        return mDarkModeEnabled;
    }
    
    /**
     * Detects if Windows is in dark mode by checking the registry.
     * @return true if Windows is in dark mode, false otherwise or if detection fails.
     */
    private boolean isSystemInDarkMode()
    {
        try
        {
            String os = System.getProperty("os.name").toLowerCase();
            System.out.println("[ColorTheme] Detecting system theme for OS: " + os);
            mLog.info("Detecting system theme for OS: " + os);
            
            if(os.contains("win"))
            {
                // Check Windows registry for dark mode setting
                // HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize\AppsUseLightTheme
                // 0x0 = Dark mode, 0x1 = Light mode
                String command = "reg query HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize /v AppsUseLightTheme";
                System.out.println("[ColorTheme] Executing: " + command);
                mLog.info("Executing registry query: " + command);
                
                Process process = Runtime.getRuntime().exec(command);
                
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
                
                String line;
                while((line = reader.readLine()) != null)
                {
                    System.out.println("[ColorTheme] Registry: " + line);
                    mLog.info("Registry output: " + line);
                    if(line.contains("AppsUseLightTheme"))
                    {
                        // Extract the value (0x0 or 0x1)
                        String[] parts = line.trim().split("\\s+");
                        if(parts.length >= 3)
                        {
                            String value = parts[parts.length - 1];
                            // 0x0 = Dark mode, 0x1 = Light mode
                            boolean isDark = "0x0".equals(value);
                            System.out.println("[ColorTheme] Result: AppsUseLightTheme=" + value + " -> " + (isDark ? "DARK MODE" : "LIGHT MODE"));
                            mLog.info("Windows theme detected - AppsUseLightTheme=" + value + " -> " + (isDark ? "DARK MODE" : "LIGHT MODE"));
                            reader.close();
                            return isDark;
                        }
                    }
                }
                reader.close();
                System.out.println("[ColorTheme] WARNING: Could not find registry value");
                mLog.warn("Could not find AppsUseLightTheme registry value - defaulting to light mode");
            }
            else
            {
                System.out.println("[ColorTheme] Non-Windows OS - defaulting to light mode");
                mLog.info("Non-Windows OS detected: " + os + " - defaulting to light mode");
            }
        }
        catch(Exception e)
        {
            System.out.println("[ColorTheme] ERROR: " + e.getMessage());
            e.printStackTrace();
            mLog.error("Exception while detecting system theme - defaulting to light mode", e);
        }
        
        // Default to light mode if detection fails or not on Windows
        System.out.println("[ColorTheme] Defaulting to LIGHT MODE");
        return false;
    }

    /**
     * Sets the dark mode enabled state.
     * @param enabled true to enable dark mode.
     */
    public void setDarkModeEnabled(boolean enabled)
    {
        mDarkModeEnabled = enabled;
        mPreferences.putBoolean(PREFERENCE_KEY_DARK_MODE_ENABLED, enabled);
        // Mark that user has explicitly set this preference
        mPreferences.putBoolean(PREFERENCE_KEY_USER_SET, true);
        notifyPreferenceUpdated();
    }
}

