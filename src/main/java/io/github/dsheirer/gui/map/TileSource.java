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

import java.util.List;

/**
 * Tiles the map loads: one style of a {@link TileServer}, resolved for the theme and the user's key.
 * @param cacheName folder of the tiles in the disk cache, e.g. osm/light
 * @param urlTemplate with {s} subdomain, {z} zoom, {x} column and {y} row placeholders, or null when unavailable
 * @param subdomains for the {s} placeholder, or empty
 * @param maxZoom highest zoom level the server provides.  Closer zoom levels enlarge these tiles.
 * @param darken true to recolor the tiles for a dark theme, for a server without a dark style
 * @param dark true when the tiles are dark, from a dark style or recolored
 * @param attribution shown on the map, as the server requires
 * @param unavailableReason shown instead of tiles when the source can't load tiles, or null
 */
public record TileSource(String cacheName, String urlTemplate, List<String> subdomains, int maxZoom, boolean darken,
                         boolean dark, String attribution, String unavailableReason)
{
    /**
     * Indicates if the source can load tiles
     */
    public boolean isAvailable()
    {
        return urlTemplate != null && unavailableReason == null;
    }

    /**
     * URL of the tile.  Spreads the tiles over the subdomains by position, so each tile always comes from the same
     * subdomain and stays in the server's and the browser-style caches.
     */
    public String getUrl(TileKey key)
    {
        String subdomain = subdomains.isEmpty() ? "" :
            subdomains.get(Math.floorMod(key.x() + key.y(), subdomains.size()));
        return urlTemplate.replace("{s}", subdomain).replace("{z}", String.valueOf(key.zoom()))
            .replace("{x}", String.valueOf(key.x())).replace("{y}", String.valueOf(key.y()));
    }
}
