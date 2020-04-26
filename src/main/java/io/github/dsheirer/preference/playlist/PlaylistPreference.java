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

package io.github.dsheirer.preference.playlist;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.directory.DirectoryPreference;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * User preferences for playlists
 */
public class PlaylistPreference extends Preference
{
    private final static Logger mLog = LoggerFactory.getLogger(PlaylistPreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(PlaylistPreference.class);

    private static final String FILE_PLAYLIST = "default.xml";
    private static final String FILE_LEGACY_PLAYLIST = "playlist_v2.xml";

    private static final String PREFERENCE_KEY_PLAYLIST = "playlist.path";
    private static final String PREFERENCE_KEY_PLAYLIST_LIST = "playlist.list";
    private Path mPlaylistPath;
    private DirectoryPreference mDirectoryPreference;

    /**
     * Constructs this preference with an update listener
     * @param updateListener to receive notifications whenever these preferences change
     */
    public PlaylistPreference(Listener<PreferenceType> updateListener, DirectoryPreference directoryPreference)
    {
        super(updateListener);
        mDirectoryPreference = directoryPreference;
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.PLAYLIST;
    }

    /**
     * Path to the current playlist
     */
    public Path getPlaylist()
    {
        if(mPlaylistPath == null)
        {
            mPlaylistPath = getPath(PREFERENCE_KEY_PLAYLIST, getDefaultPlaylistPath());
        }

        return mPlaylistPath;
    }

    /**
     * Legacy (version 2) playlist path.
     */
    public Path getLegacyPlaylist()
    {
        return mDirectoryPreference.getDirectoryPlaylist().resolve(FILE_LEGACY_PLAYLIST);
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
     * Sets the path to the playlist
     */
    public void setPlaylist(Path path)
    {
        mPlaylistPath = path;
        mPreferences.put(PREFERENCE_KEY_PLAYLIST, path.toString());
        notifyPreferenceUpdated();
    }

    /**
     * Default playlist
     */
    private Path getDefaultPlaylistPath()
    {
        return mDirectoryPreference.getDefaultPlaylistDirectory().resolve(FILE_PLAYLIST);
    }

    /**
     * List of known playlists
     */
    public List<Path> getPlaylistList()
    {
        List<Path> playlists = new ArrayList<>();

        String raw = mPreferences.get(PREFERENCE_KEY_PLAYLIST_LIST, null);

        if(raw != null && !raw.isEmpty())
        {
            for(String rawPath: Splitter.on(",").split(raw))
            {
                Path path = Paths.get(rawPath);

                if(!playlists.contains(path))
                {
                    playlists.add(path);
                }
            }
        }

        //Check that the list contains the current playlist
        boolean hasCurrent = false;

        for(Path path: playlists)
        {
            if(path.toString().contentEquals(getPlaylist().toString()))
            {
                hasCurrent = true;
                continue;
            }
        }

        if(!hasCurrent)
        {
            playlists.add(0, getPlaylist());
        }

        return playlists;
    }

    /**
     * Sets the list of known playlists
     */
    public void setPlaylistList(List<Path> playlistPaths)
    {
        if(playlistPaths != null && !playlistPaths.isEmpty())
        {
            String rawList = Joiner.on(",").join(playlistPaths);
            mPreferences.put(PREFERENCE_KEY_PLAYLIST_LIST, rawList);
        }
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
        String stringPath = mPreferences.get(key, defaultPath.toString());

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
