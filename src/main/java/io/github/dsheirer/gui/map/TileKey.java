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

package io.github.dsheirer.gui.map;

/**
 * Map tile address in the OpenStreetMap (slippy map) scheme
 * @param zoom level, 0 for the whole world in one tile
 * @param x tile column, 0 to 2^zoom - 1, west to east
 * @param y tile row, 0 to 2^zoom - 1, north to south
 */
public record TileKey(int zoom, int x, int y)
{
    /**
     * Tile that covers this tile at a lower (farther) zoom level
     * @param levels to go up, 1 or more
     * @return covering tile
     */
    public TileKey parent(int levels)
    {
        return new TileKey(zoom - levels, x >> levels, y >> levels);
    }
}
