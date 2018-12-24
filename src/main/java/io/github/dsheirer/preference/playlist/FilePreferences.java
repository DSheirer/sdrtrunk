/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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

package io.github.dsheirer.preference.playlist;

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
 * User preferences for playlists
 */
public class FilePreferences extends Preference
{
    private final static Logger mLog = LoggerFactory.getLogger(FilePreferences.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(FilePreferences.class);

    private static final String FOLDER_APPLICATION_ROOT = "SDRTrunk";
    private static final String FOLDER_PLAYLIST = "playlist";
    private static final String FILE_PLAYLIST = "default.xml";
    private static final String FILE_LEGACY_PLAYLIST = "playlist_v2.xml";

    private static final String PREFERENCE_KEY_PLAYLIST_FOLDER = "playlist.folder";
    private static final String PREFERENCE_KEY_PLAYLIST_LAST_ACCESSED = "playlist.last.accessed";
    private static final String PREFERENCE_KEY_PLAYLIST_STARTUP = "playlist.default";
    private static final String PREFERENCE_KEY_PLAYLIST_USE_LAST_ACCESSED = "playlist.use.last.accessed";

    private Path mPlaylistFolder;
    private Path mPlaylistStartupPath;
    private Path mPlaylistLastAccessedPath;
    private Boolean mUsePlaylistLastAccessedByDefault = true;

    /**
     * Constructs this preference with an update listener
     * @param updateListener to receive notifications whenever these preferences change
     */
    public FilePreferences(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.FILES;
    }


    /**
     * Path to the current playlist
     */
    public Path getPlaylist()
    {
        if(mUsePlaylistLastAccessedByDefault)
        {
            return getPlaylistLastAccessedPath();
        }
        else
        {
            return getPlaylistStartupPath();
        }
    }

    /**
     * Legacy (version 2) playlist path.
     */
    public Path getLegacyPlaylist()
    {
        return getPlaylistFolder().resolve(FILE_LEGACY_PLAYLIST);
    }

    /**
     * Lock file for the playlist when updating the file.
     */
    public Path getPlaylistLock()
    {
        String playlist = getPlaylist().toAbsolutePath().toString();
        return Paths.get(playlist + ".lck");
    }

    /**
     * Lock file for the playlist when updating the file.
     */
    public Path getPlaylistBackup()
    {
        String playlist = getPlaylist().toString();
        return Paths.get(playlist + ".backup");
    }

    /**
     * Path to the playlist to use at startup when 'use last accessed' is set to false.
     */
    public Path getPlaylistStartupPath()
    {
        if(mPlaylistStartupPath == null)
        {
            mPlaylistStartupPath = getPath(PREFERENCE_KEY_PLAYLIST_STARTUP, getDefaultPlaylistPath());
        }

        return mPlaylistStartupPath;
    }

    /**
     * Sets the path to the default playlist
     */
    public void setPlaylistStartupPath(Path path)
    {
        mPlaylistStartupPath = path;
        mPreferences.put(PREFERENCE_KEY_PLAYLIST_STARTUP, path.toString());
        notifyPreferenceUpdated();
    }

    /**
     * Path for the last accesssed playlist
     */
    public Path getPlaylistLastAccessedPath()
    {
        if(mPlaylistLastAccessedPath == null)
        {
            mPlaylistLastAccessedPath = getPath(PREFERENCE_KEY_PLAYLIST_LAST_ACCESSED, getDefaultPlaylistPath());
        }

        return mPlaylistLastAccessedPath;
    }

    /**
     * Sets the path to the last accessed playlist
     */
    public void setPlaylistLastAccessedPath(Path path)
    {
        mPlaylistLastAccessedPath = path;
        mPreferences.put(PREFERENCE_KEY_PLAYLIST_LAST_ACCESSED, path.toString());
        notifyPreferenceUpdated();
    }

    /**
     * Path to the folder for storing playlist
     */
    public Path getPlaylistFolder()
    {
        if(mPlaylistFolder == null)
        {
            mPlaylistFolder = getPath(PREFERENCE_KEY_PLAYLIST_FOLDER, getDefaultPlaylistFolder());
        }

        return mPlaylistFolder;
    }

    /**
     * Sets the path to the playlist folder
     */
    public void setPlaylistFolder(Path path)
    {
        mPlaylistFolder = path;
        mPreferences.put(PREFERENCE_KEY_PLAYLIST_FOLDER, path.toString());
        notifyPreferenceUpdated();
    }

    /**
     * Indicates the playlist to load on startup, either the last accessed playlist (true) or the default playlist (false).
     */
    public boolean usePlaylistLastAccessedByDefault()
    {
        if(mUsePlaylistLastAccessedByDefault == null)
        {
            mUsePlaylistLastAccessedByDefault = mPreferences.getBoolean(PREFERENCE_KEY_PLAYLIST_USE_LAST_ACCESSED, true);
        }

        return mUsePlaylistLastAccessedByDefault;
    }

    /**
     * Sets using the last accessed playlist by default (true) or the default playlist (false).
     */
    public void setUsePlaylistLastAccessedByDefault(boolean useLastAccessed)
    {
        mUsePlaylistLastAccessedByDefault = useLastAccessed;
        mPreferences.putBoolean(PREFERENCE_KEY_PLAYLIST_USE_LAST_ACCESSED, useLastAccessed);
        notifyPreferenceUpdated();
    }


    /**
     * Default application root path
     */
    private Path getDefaultApplicationFolder()
    {
        Path defaultApplicationPath = Paths.get(System.getProperty("user.home"), FOLDER_APPLICATION_ROOT);
        createDirectory(defaultApplicationPath);
        return defaultApplicationPath;
    }

    /**
     * Default playlist folder
     */
    public Path getDefaultPlaylistFolder()
    {
        Path defaultPlaylistFolder = getDefaultApplicationFolder().resolve(FOLDER_PLAYLIST);
        createDirectory(defaultPlaylistFolder);
        return defaultPlaylistFolder;
    }

    /**
     * Default playlist
     */
    private Path getDefaultPlaylistPath()
    {
        return getDefaultPlaylistFolder().resolve(FILE_PLAYLIST);
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
        String stringPath = mPreferences.get(PREFERENCE_KEY_PLAYLIST_STARTUP, defaultPath.toString());

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

    /**
     * Creates a directory if it does not already exist
     */
    private void createDirectory(Path directory)
    {
        if(!Files.exists(directory) && Files.isDirectory(directory))
        {
            try
            {
                Files.createDirectory(directory);
                mLog.info("Created directory [" + directory.toString() + "]");
            }
            catch(Exception e)
            {
                mLog.error("Error creating directory [" + directory.toString() + "]");
            }
        }
    }
}
