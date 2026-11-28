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
 * Stored playlist
 *
 * @param id of the playlist row
 * @param name of the playlist, unique in the schema
 * @param version of the playlist content (see PlaylistManager.PLAYLIST_CURRENT_VERSION)
 * @param created time, milliseconds since epoch
 * @param modified time of the last change, milliseconds since epoch
 * @param channels count of channels in the playlist (filled by a listing, otherwise 0)
 * @param aliases count of aliases in the playlist (filled by a listing, otherwise 0)
 */
public record PlaylistRecord(long id, String name, int version, long created, long modified, long channels,
                             long aliases)
{
}
