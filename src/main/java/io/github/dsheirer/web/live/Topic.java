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

package io.github.dsheirer.web.live;

import io.github.dsheirer.web.auth.Permission;
import java.util.Arrays;
import java.util.Optional;

/**
 * Live topics on the WebSocket, each with the permission it requires.
 */
public enum Topic
{
    /**
     * Runtime state of each processing channel timeslot
     */
    CHANNELS_STATUS("channels.status", Permission.PLAYLIST_READ),
    /**
     * Discovered tuners: status, tuning, errors
     */
    TUNERS_STATUS("tuners.status", Permission.TUNER_READ),
    /**
     * Call lifecycle: started, updated, ended (metadata only, no audio)
     */
    CALLS("calls", Permission.EVENTS_READ),
    /**
     * Decode events: created and updated
     */
    EVENTS("events", Permission.EVENTS_READ),
    /**
     * Channels and aliases of the active playlist: added, changed (with the new version), removed
     */
    PLAYLIST("playlist", Permission.PLAYLIST_READ),
    /**
     * Binary spectrum frames of a tuner
     */
    SPECTRUM("spectrum", Permission.SPECTRUM_VIEW),
    /**
     * Live call audio: audio.start and audio.end messages with binary Opus frames between them
     */
    AUDIO_LIVE("audio.live", Permission.AUDIO_LISTEN);

    private final String mName;
    private final Permission mPermission;

    Topic(String name, Permission permission)
    {
        mName = name;
        mPermission = permission;
    }

    public String getName()
    {
        return mName;
    }

    public Permission getPermission()
    {
        return mPermission;
    }

    public static Optional<Topic> fromName(String name)
    {
        return Arrays.stream(values()).filter(topic -> topic.mName.equals(name)).findFirst();
    }
}
