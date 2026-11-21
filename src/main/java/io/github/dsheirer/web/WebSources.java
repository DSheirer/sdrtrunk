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

package io.github.dsheirer.web;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.call.CallManager;
import io.github.dsheirer.event.EventManager;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.settings.identifier.TalkgroupFormatSettings;
import io.github.dsheirer.source.tuner.manager.TunerManager;

/**
 * The application components that the web API reads from.  Any of them may be null (tests, or a component that isn't
 * available), in which case the endpoints and topics that depend on it report that it isn't available.
 *
 * @param tunerManager discovered tuners
 * @param playlistManager channels and channel processing
 * @param callManager live calls
 * @param eventManager live decode events
 * @param aliasModel for alias names
 * @param talkgroupFormat for formatting identifiers in channel status
 */
public record WebSources(TunerManager tunerManager, PlaylistManager playlistManager, CallManager callManager,
                         EventManager eventManager, AliasModel aliasModel, TalkgroupFormatSettings talkgroupFormat)
{
    /**
     * No sources
     */
    public static WebSources none()
    {
        return new WebSources(null, null, null, null, null, null);
    }
}
