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

package io.github.dsheirer.settings.playlist;

import io.github.dsheirer.settings.Setting;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import io.github.dsheirer.settings.AbstractSettings;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.settings.directory.DirectorySettings;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings for playlists
 */
public class PlaylistSettings extends AbstractSettings
{
    private final static Logger mLog = LoggerFactory.getLogger(PlaylistSettings.class);

    private static final String FILE_PLAYLIST = "default.xml";
    private static final String FILE_LEGACY_PLAYLIST = "playlist_v2.xml";

    private Path mPlaylistPath;
    private DirectorySettings mDirectorySettings;

    /**
     * Constructs this setting with an update listener
     * @param updateListener to receive notifications whenever these settings change
     */
    public PlaylistSettings(Listener<SettingsType> updateListener, DirectorySettings directorySettings)
    {
        super(updateListener);
        mDirectorySettings = directorySettings;
    }

    @Override
    public SettingsType getSettingsType()
    {
        return SettingsType.PLAYLIST;
    }

    /**
     * Path to the current playlist
     */
    public Path getPlaylist()
    {
        if(mPlaylistPath == null)
        {
            mPlaylistPath = getPath(Setting.PLAYLIST_PATH, getDefaultPlaylistPath());
        }

        return mPlaylistPath;
    }

    /**
     * Legacy (version 2) playlist path.
     */
    public Path getLegacyPlaylist()
    {
        return mDirectorySettings.getDirectoryPlaylist().resolve(FILE_LEGACY_PLAYLIST);
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
        Setting.PLAYLIST_PATH.put(path.toString());
        notifySettingsUpdated();
    }

    /**
     * Default playlist
     */
    private Path getDefaultPlaylistPath()
    {
        return mDirectorySettings.getDefaultPlaylistDirectory().resolve(FILE_PLAYLIST);
    }

    /**
     * List of known playlists
     */
    public List<Path> getPlaylistList()
    {
        List<Path> playlists = new ArrayList<>();

        String raw = Setting.PLAYLIST_LIST.get(null);

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
            Setting.PLAYLIST_LIST.put(rawList);
        }
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
        String stringPath = setting.get(defaultPath.toString());

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
