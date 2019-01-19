/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.preference.decoder;

import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

/**
 * Decoder preferences
 */
public class JmbeLibraryPreference extends Preference
{
    private final static Logger mLog = LoggerFactory.getLogger(JmbeLibraryPreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(JmbeLibraryPreference.class);

    private static final String PREFERENCE_KEY_PATH_JMBE_LIBRARY = "path.jmbe.library";
    private Path mPathJmbeLibrary;

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
