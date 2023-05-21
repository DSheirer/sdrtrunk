/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.audio.broadcast;

public enum BroadcastState
{
    /**
     * General error in configuration that causes the remote server to reject the connection
     */
    CONFIGURATION_ERROR("Configuration Error", true, false),

    /**
     * Connected to the broadcastAudio server and capable of streaming
     */
    CONNECTED("Connected", false, false),

    /**
     * Connection interrupted, attempting to reconnect.
     */
    CONNECTING("Connecting", false, false),

    /**
     * Indicates the configuration is disabled
     */
    DISABLED("Disabled", true, false),

    /**
     * Disconnected from the broadcastAudio server
     */
    DISCONNECTED("Disconnected", false, false),

    /**
     * Invalid credentials - user name or password
     */
    INVALID_CREDENTIALS("Invalid User Name/Password", true, false),

    /**
     * Invalid mount point
     */
    INVALID_MOUNT_POINT("Invalid Mount/Stream ID", true, false),

    /**
     * Invalid configuration settings
     */
    INVALID_SETTINGS("Invalid Settings", true, false),

    /**
     * Remote server max sources has been exceeded
     */
    MAX_SOURCES_EXCEEDED("Max Sources Exceeded", false, true),

    /**
     * Specified mount point is already in use
     */
    MOUNT_POINT_IN_USE("Mount Point In Use", false, true),

    /**
     * Network is unavailable or the server address cannot be resolved
     */
    NETWORK_UNAVAILABLE("Network Unavailable", false, false),

    /**
     * Server is not known or reachable
     */
    NO_SERVER("No Server", false, false),

    /**
     * Initial state with no connection attempted.
     */
    READY("Ready", false, false),

    /**
     * Error while broadcasting stream data.  Temporary error state to allow connection to be reset.
     */
    TEMPORARY_BROADCAST_ERROR("Temporary Broadcast Error", false, false),

    /**
     * Unsupported audio format
     */
    UNSUPPORTED_AUDIO_FORMAT("Unsupported Audio Type", true, false),

    /**
     * Unspecified error
     */
    ERROR("Error", true, false);

    private String mLabel;
    private boolean mErrorState;
    private boolean mWarningState;

    BroadcastState(String label, boolean errorState, boolean warningState)
    {
        mLabel = label;
        mErrorState = errorState;
        mWarningState = warningState;
    }

    public String toString()
    {
        return mLabel;
    }

    public boolean isErrorState()
    {
        return mErrorState;
    }

    public boolean isWarningState()
    {
        return mWarningState;
    }
}
