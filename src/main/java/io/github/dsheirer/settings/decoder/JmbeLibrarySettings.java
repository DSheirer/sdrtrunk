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

package io.github.dsheirer.settings.decoder;

import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.jmbe.github.Version;
import io.github.dsheirer.settings.AbstractSettings;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decoder settings
 */
public class JmbeLibrarySettings extends AbstractSettings
{
    public static final boolean DEFAULT_ALERT_IF_MISSING_LIBRARY = true;
    private final static Logger mLog = LoggerFactory.getLogger(JmbeLibrarySettings.class);

    private final Pattern VERSION_PATTERN = Pattern.compile(".*jmbe-(\\d{1,5}.\\d{1,5}.\\d{1,5}\\w*)\\.jar");
    private Path mPathJmbeLibrary;
    private Boolean mAlertIfMissingLibraryRequired;

    /**
     * Constructs this setting with an update listener
     *
     * @param updateListener to receive notifications whenever these settings change
     */
    public JmbeLibrarySettings(Listener<SettingsType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public SettingsType getSettingsType()
    {
        return SettingsType.JMBE_LIBRARY;
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
            mPathJmbeLibrary = getPath(Setting.JMBE_LIBRARY_PATH, null);
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
        Setting.JMBE_LIBRARY_PATH.put(path.toString());
        notifySettingsUpdated();
    }

    /**
     * Resets (removes) the current JMBE library path
     */
    public void resetPathJmbeLibrary()
    {
        Setting.JMBE_LIBRARY_PATH.remove();
        mPathJmbeLibrary = null;
        notifySettingsUpdated();
    }

    /**
     * Indicates if the user should be alerted when the JMBE library is not setup, but required for decoding.
     */
    public boolean getAlertIfMissingLibraryRequired()
    {
        if(mAlertIfMissingLibraryRequired == null)
        {
            mAlertIfMissingLibraryRequired = Setting.JMBE_LIBRARY_ALERT_REQUIRED.getBoolean(DEFAULT_ALERT_IF_MISSING_LIBRARY);
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
        Setting.JMBE_LIBRARY_ALERT_REQUIRED.putBoolean(mAlertIfMissingLibraryRequired);
        notifySettingsUpdated();
    }

    /**
     * Returns the path stored in settings and referenced by the key argument if it exists, otherwise returns the
     * default path.
     *
     * Note: the default path is not checked for existence.
     *
     * @param setting that stores the path
     * @param defaultPath to use if the preferred path does not exist
     * @return requested path
     */
    private Path getPath(Setting setting, Path defaultPath)
    {
        String stringPath = setting.get(defaultPath != null ? defaultPath.toString() : null);

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
