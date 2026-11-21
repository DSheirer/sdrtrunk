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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Free map tile server.  A server with a dark style follows the theme; a server without one has its light tiles
 * recolored in dark themes.
 * @param id stored in the map preference.  Don't change it.
 * @param name shown to the user
 * @param lightUrl template of the light style tiles, see {@link TileSource#urlTemplate()}, with a {key} placeholder
 * when the server needs a key
 * @param darkUrl template of the dark style tiles, or null when the server has no dark style
 * @param subdomains for the {s} placeholder, or empty
 * @param maxZoom highest zoom level the server provides
 * @param attribution required by the server
 * @param keyUrl web page where the user gets a free key, or null when the server doesn't need a key
 */
public record TileServer(String id, String name, String lightUrl, String darkUrl, List<String> subdomains,
                         int maxZoom, String attribution, String keyUrl)
{
    private static final String OSM = "© OpenStreetMap contributors";

    public static final TileServer OPEN_STREET_MAP = new TileServer("osm", "OpenStreetMap",
        "https://tile.openstreetmap.org/{z}/{x}/{y}.png", null, List.of(), 19, OSM, null);

    /**
     * Positron (light) and Dark Matter (dark).  Needs a free key since 25 September 2026: 5 million tiles per month
     * for non-commercial use.
     */
    public static final TileServer CARTO = new TileServer("carto", "CARTO",
        "https://basemaps.cartocdn.com/rastertiles/light_all/{z}/{x}/{y}.png?key={key}",
        "https://basemaps.cartocdn.com/rastertiles/dark_all/{z}/{x}/{y}.png?key={key}", List.of(), 18,
        OSM + ", © CARTO", "https://carto.com/basemaps/apikey/");

    public static final TileServer ESRI_GRAY = new TileServer("esri.gray", "Esri Gray Canvas",
        "https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}",
        "https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}",
        List.of(), 16, "Tiles © Esri — Esri, HERE, Garmin, " + OSM, null);

    public static final TileServer OPEN_TOPO_MAP = new TileServer("opentopomap", "OpenTopoMap",
        "https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png", null, List.of("a", "b", "c"), 17,
        "Map data: " + OSM + ", SRTM | Map style: © OpenTopoMap (CC-BY-SA)", null);

    /**
     * Servers offered to the user, in menu order
     */
    public static final List<TileServer> SERVERS = List.of(OPEN_STREET_MAP, CARTO, ESRI_GRAY, OPEN_TOPO_MAP);

    /**
     * Server with the id
     * @param id from the map preference, or null
     * @return server, or OpenStreetMap when there's no server with the id
     */
    public static TileServer get(String id)
    {
        return SERVERS.stream().filter(server -> server.id().equals(id)).findFirst().orElse(OPEN_STREET_MAP);
    }

    /**
     * Indicates if the server needs a key from the user
     */
    public boolean requiresKey()
    {
        return keyUrl != null;
    }

    /**
     * Indicates if the server has its own dark style
     */
    public boolean hasDarkStyle()
    {
        return darkUrl != null;
    }

    /**
     * Tiles to load for the theme
     * @param dark true for a dark theme
     * @param key from the user, for a server that needs one, or null
     */
    public TileSource getSource(boolean dark, String key)
    {
        boolean darkStyle = dark && hasDarkStyle();
        String template = darkStyle ? darkUrl : lightUrl;
        String cacheName = id + (darkStyle ? "/dark" : "/light");
        String unavailable = null;

        if(requiresKey())
        {
            if(key == null || key.isBlank())
            {
                unavailable = name + " needs a free API key: enter it in the map's Settings tab";
            }
            else
            {
                template = template.replace("{key}", URLEncoder.encode(key.strip(), StandardCharsets.UTF_8));
            }
        }

        return new TileSource(cacheName, template, subdomains, maxZoom, dark && !hasDarkStyle(), dark, attribution,
            unavailable);
    }

    @Override
    public String toString()
    {
        return name;
    }
}
