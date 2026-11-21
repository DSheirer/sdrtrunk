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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static io.github.dsheirer.web.auth.Permission.*;

/**
 * Web API roles.  Each role is a fixed set of permissions; custom roles can come later without changing the endpoint
 * checks.  Roles are ordered from most to least access.
 */
public enum Role
{
    /**
     * Owns accounts, application configuration and which playlist is active
     */
    ADMIN("Admin", EnumSet.of(USERS_MANAGE, CONFIG_READ, CONFIG_WRITE, PLAYLIST_READ, PLAYLIST_WRITE,
        PLAYLIST_ACTIVATE, CHANNEL_CONTROL, TUNER_READ, TUNER_CONTROL, SPECTRUM_VIEW, STREAMING_CONTROL, AUDIO_LISTEN,
        AUDIO_DOWNLOAD, EVENTS_READ, MESSAGES_READ, MAP_READ)),

    /**
     * Runs the radio: edits channels and aliases, starts and stops channels, drives tuners and broadcasters
     */
    OPERATOR("Operator", EnumSet.of(CONFIG_READ, PLAYLIST_READ, PLAYLIST_WRITE, CHANNEL_CONTROL, TUNER_READ,
        TUNER_CONTROL, SPECTRUM_VIEW, STREAMING_CONTROL, AUDIO_LISTEN, AUDIO_DOWNLOAD, EVENTS_READ, MESSAGES_READ,
        MAP_READ)),

    /**
     * Monitors: hears audio, sees events, map and spectrum, and changes nothing
     */
    LISTENER("Listener", EnumSet.of(PLAYLIST_READ, TUNER_READ, SPECTRUM_VIEW, AUDIO_LISTEN, EVENTS_READ, MAP_READ)),

    /**
     * Sees events and the map with no audio, for dashboards or status screens
     */
    VIEWER("Viewer", EnumSet.of(PLAYLIST_READ, TUNER_READ, EVENTS_READ, MAP_READ));

    private final String mLabel;
    private final Set<Permission> mPermissions;

    Role(String label, EnumSet<Permission> permissions)
    {
        mLabel = label;
        mPermissions = Collections.unmodifiableSet(permissions);
    }

    public String getLabel()
    {
        return mLabel;
    }

    /**
     * Permissions granted by this role
     */
    public Set<Permission> getPermissions()
    {
        return mPermissions;
    }

    /**
     * Parses a stored or requested role name, ignoring case.
     */
    public static Optional<Role> parse(String name)
    {
        if(name == null)
        {
            return Optional.empty();
        }

        try
        {
            return Optional.of(Role.valueOf(name.trim().toUpperCase(Locale.ROOT)));
        }
        catch(IllegalArgumentException iae)
        {
            return Optional.empty();
        }
    }
}
