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

package io.github.dsheirer.database.playlist;

/**
 * One stored playlist item: a channel, alias, alias list configuration, broadcast (streaming) configuration or
 * channel map, serialized with the same Jackson XML that the playlist files use.
 *
 * @param id of the row, or 0 when not stored yet
 * @param playlistId of the playlist that holds the item
 * @param type of item
 * @param name of the item (channel name, alias name, etc.) for display and searching, may be null
 * @param config serialized item
 */
public record PlaylistItem(long id, long playlistId, PlaylistItemType type, String name, String config)
{
    /**
     * Copy of this item with the row id
     */
    public PlaylistItem withId(long id)
    {
        return new PlaylistItem(id, playlistId, type, name, config);
    }

    /**
     * Copy of this item in the playlist
     */
    public PlaylistItem withPlaylistId(long playlistId)
    {
        return new PlaylistItem(id, playlistId, type, name, config);
    }
}
