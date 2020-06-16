/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.preference.decoder;

import io.github.dsheirer.jmbe.github.Version;
import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decoder preferences
 */
public class JmbeLibraryPreference extends Preference
{
    private final static Logger mLog = LoggerFactory.getLogger(JmbeLibraryPreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(JmbeLibraryPreference.class);

    private static final String PREFERENCE_KEY_PATH_JMBE_LIBRARY = "path.jmbe.library.1.0.0";
    private static final String PREFERENCE_KEY_PATH_ALERT_LIBRARY_REQUIRED = "alert.jmbe.required";
    private final Pattern VERSION_PATTERN = Pattern.compile(".*jmbe-(\\d{1,5}.\\d{1,5}.\\d{1,5}\\w*)\\.jar");
    private Path mPathJmbeLibrary;
    private Boolean mAlertIfMissingLibraryRequired;

    /**
     * Constructs this preference with an update listener
     *
     * @param updateListener to receive notifications whenever these preferences change
     */
    public JmbeLibraryPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.JMBE_LIBRARY;
    }

    public Version getCurrentVersion()
    {
        Path path = getPathJmbeLibrary();

        if(path != null)
        {
            Matcher m = VERSION_PATTERN.matcher(path.toString());

            if(m.matches())
            {
                return Version.fromString(m.group(1));
            }
        }

        return null;
    }

    /*
     * Path to the JMBE audio codec library
     */
    public Path getPathJmbeLibrary()
    {
        if(mPathJmbeLibrary == null)
        {
            mPathJmbeLibrary = getPath(PREFERENCE_KEY_PATH_JMBE_LIBRARY, null);
        }

        return mPathJmbeLibrary;
    }

    /**
     * Indicates if a path to a JMBE library is setup (ie non-null).
     */
    public boolean hasJmbeLibraryPath()
    {
        return getPathJmbeLibrary() != null;
    }

    /**
     * Sets the path to the JMBE library
     */
    public void setPathJmbeLibrary(Path path)
    {
        mPathJmbeLibrary = path;
        mPreferences.put(PREFERENCE_KEY_PATH_JMBE_LIBRARY, path.toString());
        notifyPreferenceUpdated();
    }

    /**
     * Resets (removes) the current JMBE library path
     */
    public void resetPathJmbeLibrary()
    {
        mPreferences.remove(PREFERENCE_KEY_PATH_JMBE_LIBRARY);
        mPathJmbeLibrary = null;
        notifyPreferenceUpdated();
    }

    /**
     * Indicates if the user should be alerted when the JMBE library is not setup, but required for decoding.
     */
    public boolean getAlertIfMissingLibraryRequired()
    {
        if(mAlertIfMissingLibraryRequired == null)
        {
            mAlertIfMissingLibraryRequired = mPreferences.getBoolean(PREFERENCE_KEY_PATH_ALERT_LIBRARY_REQUIRED, true);
        }

        return mAlertIfMissingLibraryRequired;
    }

    /**
     * Sets the value for alerting when the library is required for decoding but the library is not currently setup.
     * @param alert true if the user should be alerted when the library is not setup, but required for decoding.
     */
    public void setAlertIfMissingLibraryRequired(boolean alert)
    {
        mAlertIfMissingLibraryRequired = alert;
        mPreferences.putBoolean(PREFERENCE_KEY_PATH_ALERT_LIBRARY_REQUIRED, mAlertIfMissingLibraryRequired);
        notifyPreferenceUpdated();
    }

    /**
     * Returns the path stored in preferences and referenced by the key argument if it exists, otherwise returns the
     * default path.
     *
     * Note: the default path is not checked for existence.
     *
     * @param key to the preferences service for the path
     * @param defaultPath to use if the preferred path does not exist
     * @return requested path
     */
    private Path getPath(String key, Path defaultPath)
    {
        String stringPath = mPreferences.get(key, defaultPath != null ? defaultPath.toString() : null);

        if(stringPath != null && !stringPath.isEmpty())
        {
            Path temp = Paths.get(stringPath);

            if(Files.exists(temp))
            {
                return temp;
            }
        }

        return defaultPath;
    }
}
