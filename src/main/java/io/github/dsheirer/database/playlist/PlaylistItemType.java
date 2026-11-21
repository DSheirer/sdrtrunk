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
 * Kind of playlist item stored in a playlist item row
 */
public enum PlaylistItemType
{
    ALIAS,
    ALIAS_LIST,
    BROADCAST,
    CHANNEL,
    CHANNEL_MAP;

    /**
     * Type for the stored name, or null if the name isn't a known type (a row written by a newer version).
     */
    public static PlaylistItemType fromName(String name)
    {
        for(PlaylistItemType type: values())
        {
            if(type.name().equals(name))
            {
                return type;
            }
        }

        return null;
    }
}
