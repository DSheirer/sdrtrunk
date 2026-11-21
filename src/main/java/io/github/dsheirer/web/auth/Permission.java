/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.web.auth;

import io.javalin.security.RouteRole;
import java.util.Arrays;
import java.util.Optional;

/**
 * Web API permissions.  Each endpoint and WebSocket topic requires one permission, and each role is a fixed set of
 * permissions (see {@link Role}).
 *
 * Endpoints declare their permission as a Javalin route role, and the {@link AccessManager} checks it before the
 * handler runs.  {@link #AUTHENTICATED} and {@link #PUBLIC} aren't granted by roles: they mark endpoints that any
 * signed-in user, or anyone, may call.
 */
public enum Permission implements RouteRole
{
    USERS_MANAGE("users.manage", "Manage accounts and read the audit log"),
    CONFIG_READ("config.read", "Read the application configuration"),
    CONFIG_WRITE("config.write", "Change logging, directories and settings"),
    PLAYLIST_READ("playlist.read", "Read playlists, channels and aliases"),
    PLAYLIST_WRITE("playlist.write", "Edit channels, aliases and broadcast configurations"),
    PLAYLIST_ACTIVATE("playlist.activate", "Switch the active playlist"),
    CHANNEL_CONTROL("channel.control", "Start, stop and auto-start channels"),
    TUNER_READ("tuner.read", "Read tuners and their status"),
    TUNER_CONTROL("tuner.control", "Change tuner frequency, gain, sample rate and enable"),
    SPECTRUM_VIEW("spectrum.view", "View the spectrum and waterfall"),
    STREAMING_CONTROL("streaming.control", "Start, stop and configure audio streaming"),
    AUDIO_LISTEN("audio.listen", "Listen to live and recorded call audio"),
    AUDIO_DOWNLOAD("audio.download", "Download call audio and recordings"),
    EVENTS_READ("events.read", "Read decode events and calls"),
    MESSAGES_READ("messages.read", "Read decoded message streams"),
    MAP_READ("map.read", "Read the map"),

    /**
     * Any signed-in user, for example to read their own account or change their own password
     */
    AUTHENTICATED("authenticated", "Any signed-in user"),

    /**
     * No sign-in needed
     */
    PUBLIC("public", "No sign-in needed");

    private final String mKey;
    private final String mDescription;

    Permission(String key, String description)
    {
        mKey = key;
        mDescription = description;
    }

    /**
     * Key used in the API, for example playlist.read
     */
    public String getKey()
    {
        return mKey;
    }

    public String getDescription()
    {
        return mDescription;
    }

    /**
     * Indicates if a role can grant this permission.  False for the authenticated and public markers.
     */
    public boolean isGrantable()
    {
        return this != AUTHENTICATED && this != PUBLIC;
    }

    /**
     * Finds the permission with the key.
     */
    public static Optional<Permission> fromKey(String key)
    {
        return Arrays.stream(values()).filter(permission -> permission.mKey.equals(key)).findFirst();
    }

    @Override
    public String toString()
    {
        return mKey;
    }
}
