/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.settings.directory;

import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.AbstractSettings;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.sample.Listener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings for directories
 */
public class DirectorySettings extends AbstractSettings
{
    private final static Logger mLog = LoggerFactory.getLogger(DirectorySettings.class);

    public static final int DEFAULT_USAGE_THRESHOLD_RECORDINGS_MB = 2000;
    public static final int DEFAULT_USAGE_THRESHOLD_EVENT_LOGS_MB = 200;

    private static final String DIRECTORY_APPLICATION_ROOT = "SDRTrunk";
    private static final String DIRECTORY_APPLICATION_LOG = "logs";
    private static final String DIRECTORY_CONFIGURATION = "configuration";
    private static final String DIRECTORY_EVENT_LOG = "event_logs";
    private static final String DIRECTORY_JMBE = "jmbe";
    private static final String DIRECTORY_PLAYLIST = "playlist";
    private static final String DIRECTORY_RECORDING = "recordings";
    private static final String DIRECTORY_SCREEN_CAPTURE = "screen_captures";
    private static final String DIRECTORY_STREAMING = "streaming";


    private Path mDirectoryApplicationRoot;
    private Path mDirectoryApplicationLogs;
    private Path mDirectoryConfiguration;
    private Path mDirectoryEventLogs;
    private Path mDirectoryJmbe;
    private Path mDirectoryPlaylist;
    private Path mDirectoryRecording;
    private Path mDirectoryScreenCapture;
    private Path mDirectoryStreaming;
    private Integer mDirectoryMaxUsageRecordings;
    private Integer mDirectoryMaxUsageEventLogs;

    /**
     * Constructs this setting with an update listener
     * @param updateListener to receive notifications whenever these settings change
     */
    public DirectorySettings(Listener<SettingsType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public SettingsType getSettingsType()
    {
        return SettingsType.DIRECTORY;
    }

    /**
     * Path to the application root directory
     */
    public Path getDirectoryApplicationRoot()
    {
        if(mDirectoryApplicationRoot == null)
        {
            mDirectoryApplicationRoot = getPath(Setting.DIRECTORY_APPLICATION_ROOT, getDefaultApplicationDirectory());
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
        Setting.DIRECTORY_APPLICATION_ROOT.put(path.toString());

        //Set the child paths to null so that they'll be recreated on next access.  If the user has explicitly set a
        //path override for any of these directories, then it will be retained.
        nullifyApplicationChildDirectories();

        notifySettingsUpdated();
    }

    /**
     * Nullifies each of the application root child directories so that they can be re-created from application root
     * or re-created from a persisted override path.
     */
    private void nullifyApplicationChildDirectories()
    {
        mDirectoryApplicationLogs = null;
        mDirectoryConfiguration = null;
        mDirectoryEventLogs = null;
        mDirectoryJmbe = null;
        mDirectoryPlaylist = null;
        mDirectoryRecording = null;
        mDirectoryScreenCapture = null;
        mDirectoryStreaming = null;
    }

    /**
     * User-specified maximum drive space usage for the recording directory.
     * @return max usage threshold in MB.
     */
    public int getDirectoryMaxUsageRecordings()
    {
        if(mDirectoryMaxUsageRecordings == null)
        {
            mDirectoryMaxUsageRecordings = Setting.DIRECTORY_MAX_USAGE_RECORDINGS.getInt(DEFAULT_USAGE_THRESHOLD_RECORDINGS_MB);
        }

        return mDirectoryMaxUsageRecordings;
    }

    /**
     * Sets the threshold for maximum drive space usage for the recordings directory.
     * @param thresholdMB threshold in MB
     */
    public void setDirectoryMaxUsageRecordings(int thresholdMB)
    {
        Setting.DIRECTORY_MAX_USAGE_RECORDINGS.putInt(thresholdMB);
        mDirectoryMaxUsageRecordings = thresholdMB;
        notifySettingsUpdated();
    }

    /**
     * User-specified maximum drive space usage for the event logs directory.
     * @return max usage threshold in MB.
     */
    public int getDirectoryMaxUsageEventLogs()
    {
        if(mDirectoryMaxUsageEventLogs == null)
        {
            mDirectoryMaxUsageEventLogs = Setting.DIRECTORY_MAX_USAGE_EVENT_LOGS.getInt(DEFAULT_USAGE_THRESHOLD_EVENT_LOGS_MB);
        }

        return mDirectoryMaxUsageEventLogs;
    }

    /**
     * Sets the threshold for maximum drive space usage for the event logs directory.
     * @param thresholdMB threshold in MB
     */
    public void setDirectoryMaxUsageEventLogs(int thresholdMB)
    {
        Setting.DIRECTORY_MAX_USAGE_EVENT_LOGS.putInt(thresholdMB);
        mDirectoryMaxUsageEventLogs = thresholdMB;
        notifySettingsUpdated();
    }

    /**
     * Removes a stored recording directory setting so that the default path can be used again
     */
    public void resetDirectoryApplicationRoot()
    {
        mDirectoryApplicationRoot = null;
        Setting.DIRECTORY_APPLICATION_ROOT.remove();

        //Set the child paths to null so that they'll be recreated on next access.  If the user has explicitly set a
        //path override for any of these directories, then it will be retained.
        nullifyApplicationChildDirectories();

        notifySettingsUpdated();
    }

    /**
     * Path to the folder for storing application logs
     */
    public Path getDirectoryApplicationLog()
    {
        if(mDirectoryApplicationLogs == null)
        {
            mDirectoryApplicationLogs = getPath(Setting.DIRECTORY_APPLICATION_LOGS, getDefaultApplicationLogsDirectory());
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
        Setting.DIRECTORY_APPLICATION_LOGS.put(path.toString());
        notifySettingsUpdated();
    }

    /**
     * Removes a stored application logs directory setting so that the default path can be used again
     */
    public void resetDirectoryApplicationLogs()
    {
        Setting.DIRECTORY_APPLICATION_LOGS.remove();
        mDirectoryApplicationLogs = null;
        notifySettingsUpdated();
    }

    /**
     * Path to the folder for storing configuration files
     */
    public Path getDirectoryConfiguration()
    {
        if(mDirectoryConfiguration == null)
        {
            mDirectoryConfiguration = getPath(Setting.DIRECTORY_CONFIGURATION, getDefaultConfigurationDirectory());
            createDirectory(mDirectoryConfiguration);
        }

        return mDirectoryConfiguration;
    }

    /**
     * Sets the path to the configuration files folder
     */
    public void setDirectoryConfiguration(Path path)
    {
        mDirectoryConfiguration = path;
        Setting.DIRECTORY_CONFIGURATION.put(path.toString());
        notifySettingsUpdated();
    }

    /**
     * Removes a stored configuration directory setting so that the default path can be used again
     */
    public void resetDirectoryConfiguration()
    {
        Setting.DIRECTORY_CONFIGURATION.remove();
        mDirectoryConfiguration = null;
        notifySettingsUpdated();
    }

    /**
     * Path to the folder for storing event logs
     */
    public Path getDirectoryEventLog()
    {
        if(mDirectoryEventLogs == null)
        {
            mDirectoryEventLogs = getPath(Setting.DIRECTORY_EVENT_LOGS, getDefaultEventLogsDirectory());
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
        Setting.DIRECTORY_EVENT_LOGS.put(path.toString());
        notifySettingsUpdated();
    }

    /**
     * Removes a stored event logs directory setting so that the default path can be used again
     */
    public void resetDirectoryEventLogs()
    {
        Setting.DIRECTORY_EVENT_LOGS.remove();
        mDirectoryEventLogs = null;
        notifySettingsUpdated();
    }

    /**
     * Path to the folder for storing the JMBE audio library
     */
    public Path getDirectoryJmbe()
    {
        if(mDirectoryJmbe == null)
        {
            mDirectoryJmbe = getPath(Setting.DIRECTORY_JMBE, getDefaultJmbeDirectory());
            createDirectory(mDirectoryJmbe);
        }

        return mDirectoryJmbe;
    }

    /**
     * Sets the path to the JMBE folder
     */
    public void setDirectoryJmbe(Path path)
    {
        mDirectoryJmbe = path;
        Setting.DIRECTORY_JMBE.put(path.toString());
        notifySettingsUpdated();
    }

    /**
     * Removes a stored playlist directory setting so that the default path can be used again
     */
    public void resetDirectoryJmbe()
    {
        Setting.DIRECTORY_JMBE.remove();
        mDirectoryJmbe = null;
        notifySettingsUpdated();
    }

    /**
     * Path to the folder for storing playlist
     */
    public Path getDirectoryPlaylist()
    {
        if(mDirectoryPlaylist == null)
        {
            mDirectoryPlaylist = getPath(Setting.DIRECTORY_PLAYLIST, getDefaultPlaylistDirectory());
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
        Setting.DIRECTORY_PLAYLIST.put(path.toString());
        notifySettingsUpdated();
    }

    /**
     * Removes a stored playlist directory setting so that the default path can be used again
     */
    public void resetDirectoryPlaylist()
    {
        Setting.DIRECTORY_PLAYLIST.remove();
        mDirectoryPlaylist = null;
        notifySettingsUpdated();
    }

    /**
     * Path to the folder for storing recordings
     */
    public Path getDirectoryRecording()
    {
        if(mDirectoryRecording == null)
        {
            mDirectoryRecording = getPath(Setting.DIRECTORY_RECORDING, getDefaultRecordingDirectory());
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
        Setting.DIRECTORY_RECORDING.put(path.toString());
        notifySettingsUpdated();
    }

    /**
     * Removes a stored recording directory setting so that the default path can be used again
     */
    public void resetDirectoryRecording()
    {
        Setting.DIRECTORY_RECORDING.remove();
        mDirectoryRecording = null;
        notifySettingsUpdated();
    }

    /**
     * Path to the folder for storing screen capture
     */
    public Path getDirectoryScreenCapture()
    {
        if(mDirectoryScreenCapture == null)
        {
            mDirectoryScreenCapture = getPath(Setting.DIRECTORY_SCREEN_CAPTURE, getDefaultScreenCaptureDirectory());
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
        Setting.DIRECTORY_SCREEN_CAPTURE.put(path.toString());
        notifySettingsUpdated();
    }

    /**
     * Removes a stored screen capture directory setting so that the default path can be used again
     */
    public void resetDirectoryScreenCapture()
    {
        Setting.DIRECTORY_SCREEN_CAPTURE.remove();
        mDirectoryScreenCapture = null;
        notifySettingsUpdated();
    }

    /**
     * Path to the folder for storing streaming temporary recordings
     */
    public Path getDirectoryStreaming()
    {
        if(mDirectoryStreaming == null)
        {
            mDirectoryStreaming = getPath(Setting.DIRECTORY_STREAMING, getDefaultStreamingDirectory());
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
        Setting.DIRECTORY_STREAMING.put(path.toString());
        notifySettingsUpdated();
    }

    /**
     * Removes a stored streaming directory setting so that the default path can be used again
     */
    public void resetDirectoryStreaming()
    {
        Setting.DIRECTORY_STREAMING.remove();
        mDirectoryStreaming = null;
        notifySettingsUpdated();
    }

    /**
     * Default application root directory
     */
    public Path getDefaultApplicationDirectory()
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
     * Default configuration directory
     */
    public Path getDefaultConfigurationDirectory()
    {
        return getDirectoryApplicationRoot().resolve(DIRECTORY_CONFIGURATION);
    }

    /**
     * Default event logs directory
     */
    public Path getDefaultEventLogsDirectory()
    {
        return getDirectoryApplicationRoot().resolve(DIRECTORY_EVENT_LOG);
    }

    /**
     * Default JMBE directory
     */
    public Path getDefaultJmbeDirectory()
    {
        return getDirectoryApplicationRoot().resolve(DIRECTORY_JMBE);
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
