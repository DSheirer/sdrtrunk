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

package io.github.dsheirer.preference.directory;

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
 * User preferences for directories
 */
public class DirectoryPreference extends Preference
{
    private final static Logger mLog = LoggerFactory.getLogger(DirectoryPreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(DirectoryPreference.class);

    private static final String DIRECTORY_APPLICATION_ROOT = "SDRTrunk";
    private static final String DIRECTORY_APPLICATION_LOG = "logs";
    private static final String DIRECTORY_EVENT_LOG = "event_logs";
    private static final String DIRECTORY_PLAYLIST = "playlist";
    private static final String DIRECTORY_RECORDING = "recordings";
    private static final String DIRECTORY_SCREEN_CAPTURE = "screen_captures";
    private static final String DIRECTORY_STREAMING = "streaming";

    private static final String PREFERENCE_KEY_DIRECTORY_APPLICATION_ROOT = "directory.application.root";
    private static final String PREFERENCE_KEY_DIRECTORY_APPLICATION_LOGS = "directory.application.logs";
    private static final String PREFERENCE_KEY_DIRECTORY_EVENT_LOGS = "directory.event.logs";
    private static final String PREFERENCE_KEY_DIRECTORY_PLAYLIST = "directory.playlist";
    private static final String PREFERENCE_KEY_DIRECTORY_RECORDING = "directory.recording";
    private static final String PREFERENCE_KEY_DIRECTORY_SCREEN_CAPTURE = "directory.screen.capture";
    private static final String PREFERENCE_KEY_DIRECTORY_STREAMING = "directory.streaming";

    private Path mDirectoryApplicationRoot;
    private Path mDirectoryApplicationLogs;
    private Path mDirectoryEventLogs;
    private Path mDirectoryPlaylist;
    private Path mDirectoryRecording;
    private Path mDirectoryScreenCapture;
    private Path mDirectoryStreaming;

    /**
     * Constructs this preference with an update listener
     * @param updateListener to receive notifications whenever these preferences change
     */
    public DirectoryPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.DIRECTORY;
    }

    /**
     * Path to the application root directory
     */
    public Path getDirectoryApplicationRoot()
    {
        if(mDirectoryApplicationRoot == null)
        {
            mDirectoryApplicationRoot = getPath(PREFERENCE_KEY_DIRECTORY_APPLICATION_ROOT, getDefaultApplicationDirectory());
            createDirectory(mDirectoryApplicationRoot);
        }

        return mDirectoryApplicationRoot;
    }

    /**
     * Sets the path to the application root directory
     */
    public void setDirectoryApplicationRoot(Path path)
    {
        mDirectoryApplicationRoot = path;
        mPreferences.put(PREFERENCE_KEY_DIRECTORY_APPLICATION_ROOT, path.toString());

        //Set the child paths to null so that they'll be recreated on next access.  If the user has explicitly set a
        //path override for any of these directories, then it will be retained.
        nullifyApplicationChildDirectories();

        notifyPreferenceUpdated();
    }

    /**
     * Nullifies each of the application root child directories so that they can be re-created from application root
     * or re-created from a persisted override path.
     */
    private void nullifyApplicationChildDirectories()
    {
        mDirectoryApplicationLogs = null;
        mDirectoryEventLogs = null;
        mDirectoryPlaylist = null;
        mDirectoryRecording = null;
        mDirectoryScreenCapture = null;
        mDirectoryStreaming = null;
    }

    /**
     * Removes a stored recording directory preference so that the default path can be used again
     */
    public void resetDirectoryApplicationRoot()
    {
        mDirectoryApplicationRoot = null;
        mPreferences.remove(PREFERENCE_KEY_DIRECTORY_APPLICATION_ROOT);

        //Set the child paths to null so that they'll be recreated on next access.  If the user has explicitly set a
        //path override for any of these directories, then it will be retained.
        nullifyApplicationChildDirectories();

        notifyPreferenceUpdated();
    }

    /**
     * Path to the folder for storing application logs
     */
    public Path getDirectoryApplicationLog()
    {
        if(mDirectoryApplicationLogs == null)
        {
            mDirectoryApplicationLogs = getPath(PREFERENCE_KEY_DIRECTORY_APPLICATION_LOGS, getDefaultApplicationLogsDirectory());
            createDirectory(mDirectoryApplicationLogs);
        }

        return mDirectoryApplicationLogs;
    }

    /**
     * Sets the path to the application logs folder
     */
    public void setDirectoryApplicationLogs(Path path)
    {
        mDirectoryApplicationLogs = path;
        mPreferences.put(PREFERENCE_KEY_DIRECTORY_APPLICATION_LOGS, path.toString());
        notifyPreferenceUpdated();
    }

    /**
     * Removes a stored application logs directory preference so that the default path can be used again
     */
    public void resetDirectoryApplicationLogs()
    {
        mPreferences.remove(PREFERENCE_KEY_DIRECTORY_APPLICATION_LOGS);
        mDirectoryApplicationLogs = null;
        notifyPreferenceUpdated();
    }

    /**
     * Path to the folder for storing event logs
     */
    public Path getDirectoryEventLog()
    {
        if(mDirectoryEventLogs == null)
        {
            mDirectoryEventLogs = getPath(PREFERENCE_KEY_DIRECTORY_EVENT_LOGS, getDefaultEventLogsDirectory());
            createDirectory(mDirectoryEventLogs);
        }

        return mDirectoryEventLogs;
    }

    /**
     * Sets the path to the event logs folder
     */
    public void setDirectoryEventLogs(Path path)
    {
        mDirectoryEventLogs = path;
        mPreferences.put(PREFERENCE_KEY_DIRECTORY_EVENT_LOGS, path.toString());
        notifyPreferenceUpdated();
    }

    /**
     * Removes a stored playlist directory preference so that the default path can be used again
     */
    public void resetDirectoryEventLogs()
    {
        mPreferences.remove(PREFERENCE_KEY_DIRECTORY_EVENT_LOGS);
        mDirectoryEventLogs = null;
        notifyPreferenceUpdated();
    }

    /**
     * Path to the folder for storing playlist
     */
    public Path getDirectoryPlaylist()
    {
        if(mDirectoryPlaylist == null)
        {
            mDirectoryPlaylist = getPath(PREFERENCE_KEY_DIRECTORY_PLAYLIST, getDefaultPlaylistDirectory());
            createDirectory(mDirectoryPlaylist);
        }

        return mDirectoryPlaylist;
    }

    /**
     * Sets the path to the playlist folder
     */
    public void setDirectoryPlaylist(Path path)
    {
        mDirectoryPlaylist = path;
        mPreferences.put(PREFERENCE_KEY_DIRECTORY_PLAYLIST, path.toString());
        notifyPreferenceUpdated();
    }

    /**
     * Removes a stored playlist directory preference so that the default path can be used again
     */
    public void resetDirectoryPlaylist()
    {
        mPreferences.remove(PREFERENCE_KEY_DIRECTORY_PLAYLIST);
        mDirectoryPlaylist = null;
        notifyPreferenceUpdated();
    }

    /**
     * Path to the folder for storing recordings
     */
    public Path getDirectoryRecording()
    {
        if(mDirectoryRecording == null)
        {
            mDirectoryRecording = getPath(PREFERENCE_KEY_DIRECTORY_RECORDING, getDefaultRecordingDirectory());
            createDirectory(mDirectoryRecording);
        }

        return mDirectoryRecording;
    }

    /**
     * Sets the path to the playlist folder
     */
    public void setDirectoryRecording(Path path)
    {
        mDirectoryRecording = path;
        mPreferences.put(PREFERENCE_KEY_DIRECTORY_RECORDING, path.toString());
        notifyPreferenceUpdated();
    }

    /**
     * Removes a stored recording directory preference so that the default path can be used again
     */
    public void resetDirectoryRecording()
    {
        mPreferences.remove(PREFERENCE_KEY_DIRECTORY_RECORDING);
        mDirectoryRecording = null;
        notifyPreferenceUpdated();
    }

    /**
     * Path to the folder for storing screen capture
     */
    public Path getDirectoryScreenCapture()
    {
        if(mDirectoryScreenCapture == null)
        {
            mDirectoryScreenCapture = getPath(PREFERENCE_KEY_DIRECTORY_SCREEN_CAPTURE, getDefaultScreenCaptureDirectory());
            createDirectory(mDirectoryScreenCapture);
        }

        return mDirectoryScreenCapture;
    }

    /**
     * Sets the path to the screen capture folder
     */
    public void setDirectoryScreenCapture(Path path)
    {
        mDirectoryScreenCapture = path;
        mPreferences.put(PREFERENCE_KEY_DIRECTORY_SCREEN_CAPTURE, path.toString());
        notifyPreferenceUpdated();
    }

    /**
     * Removes a stored screen capture directory preference so that the default path can be used again
     */
    public void resetDirectoryScreenCapture()
    {
        mPreferences.remove(PREFERENCE_KEY_DIRECTORY_SCREEN_CAPTURE);
        mDirectoryScreenCapture = null;
        notifyPreferenceUpdated();
    }

    /**
     * Path to the folder for storing streaming temporary recordings
     */
    public Path getDirectoryStreaming()
    {
        if(mDirectoryStreaming == null)
        {
            mDirectoryStreaming = getPath(PREFERENCE_KEY_DIRECTORY_STREAMING, getDefaultStreamingDirectory());
            createDirectory(mDirectoryStreaming);
        }

        return mDirectoryStreaming;
    }

    /**
     * Sets the path to the streaming folder
     */
    public void setDirectoryStreaming(Path path)
    {
        mDirectoryStreaming = path;
        mPreferences.put(PREFERENCE_KEY_DIRECTORY_STREAMING, path.toString());
        notifyPreferenceUpdated();
    }

    /**
     * Removes a stored streaming directory preference so that the default path can be used again
     */
    public void resetDirectoryStreaming()
    {
        mPreferences.remove(PREFERENCE_KEY_DIRECTORY_STREAMING);
        mDirectoryStreaming = null;
        notifyPreferenceUpdated();
    }

    /**
     * Default application root directory
     */
    private Path getDefaultApplicationDirectory()
    {
        return Paths.get(System.getProperty("user.home"), DIRECTORY_APPLICATION_ROOT);
    }

    /**
     * Default event logs directory
     */
    public Path getDefaultApplicationLogsDirectory()
    {
        return getDirectoryApplicationRoot().resolve(DIRECTORY_APPLICATION_LOG);
    }

    /**
     * Default event logs directory
     */
    public Path getDefaultEventLogsDirectory()
    {
        return getDirectoryApplicationRoot().resolve(DIRECTORY_EVENT_LOG);
    }

    /**
     * Default playlist directory
     */
    public Path getDefaultPlaylistDirectory()
    {
        return getDirectoryApplicationRoot().resolve(DIRECTORY_PLAYLIST);
    }

    /**
     * Default playlist directory
     */
    public Path getDefaultRecordingDirectory()
    {
        return getDirectoryApplicationRoot().resolve(DIRECTORY_RECORDING);
    }

    /**
     * Default screen capture directory
     */
    public Path getDefaultScreenCaptureDirectory()
    {
        return getDirectoryApplicationRoot().resolve(DIRECTORY_SCREEN_CAPTURE);
    }

    /**
     * Default streaming directory
     */
    public Path getDefaultStreamingDirectory()
    {
        return getDirectoryApplicationRoot().resolve(DIRECTORY_STREAMING);
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

    /**
     * Creates a directory if it does not already exist
     */
    private void createDirectory(Path directory)
    {
        if(!Files.exists(directory))
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
