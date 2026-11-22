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

package io.github.dsheirer.settings.map;

import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.AbstractSettings;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.sample.Listener;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Map settings: the last center point and zoom level of the map, restored when the application starts, and the
 * map layers to show.  The user's own location is a radio reference setting, shown on the map as a pin.
 */
public class MapSettings extends AbstractSettings
{
    public static final boolean DEFAULT_SHOW_RADIO_REFERENCE_SITES = true;
    private static final Logger mLog = LoggerFactory.getLogger(MapSettings.class);

    /**
     * Zoom level when none is stored
     */
    public static final int DEFAULT_ZOOM = 7;

    /**
     * Range of OpenStreetMap tile zoom levels the map shows
     */
    public static final int MINIMUM_TILE_ZOOM = 2;
    public static final int MAXIMUM_TILE_ZOOM = 19;
    /**
     * Tile zoom level plus the stored zoom level, for the original Swing map viewer's inverted zoom scale
     */
    private static final int TILE_ZOOM_OFFSET = 19;

    /**
     * Map center point
     * @param latitude in decimal degrees, -90 to 90
     * @param longitude in decimal degrees, -180 to 180
     */
    public record Center(double latitude, double longitude)
    {
        /**
         * Constructs an instance
         * @throws IllegalArgumentException if the coordinates are out of range
         */
        public Center
        {
            if(!Double.isFinite(latitude) || !Double.isFinite(longitude) || Math.abs(latitude) > 90.0 ||
                Math.abs(longitude) > 180.0)
            {
                throw new IllegalArgumentException("Invalid map center: " + latitude + ", " + longitude);
            }
        }
    }

    /**
     * Constructs an instance
     * @param updateListener to receive notifications that a setting has been updated
     */
    public MapSettings(Listener<SettingsType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public SettingsType getSettingsType()
    {
        return SettingsType.MAP;
    }

    /**
     * Last center point of the map.  Can be called from any thread.
     * @return center, or null if none is stored
     */
    public Center getCenter()
    {
        String value = Setting.MAP_CENTER.get(null);

        if(value != null)
        {
            String[] parts = value.split(",");

            if(parts.length == 2)
            {
                try
                {
                    return new Center(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
                }
                catch(IllegalArgumentException e)
                {
                    mLog.warn("Ignoring invalid stored map center [" + value + "]");
                }
            }
        }

        return null;
    }

    /**
     * Stores the center point of the map
     * @param center to store
     */
    public void setCenter(Center center)
    {
        if(center != null && !Objects.equals(center, getCenter()))
        {
            //Stored as one value so that a reader never sees the latitude of one point with the longitude of another
            Setting.MAP_CENTER.put(center.latitude() + "," + center.longitude());
            notifySettingsUpdated();
        }
    }

    /**
     * Last zoom level of the map.  Can be called from any thread.
     * @return zoom level, or {@link #DEFAULT_ZOOM} if none is stored
     */
    public int getZoom()
    {
        return Setting.MAP_ZOOM.getInt(DEFAULT_ZOOM);
    }

    /**
     * Stores the zoom level of the map
     * @param zoom level
     */
    public void setZoom(int zoom)
    {
        if(zoom != getZoom())
        {
            Setting.MAP_ZOOM.putInt(zoom);
            notifySettingsUpdated();
        }
    }

    /**
     * Last zoom level of the map as an OpenStreetMap tile zoom level, where 0 shows the whole world in one tile and
     * higher levels are closer.  The stored zoom level uses the inverted scale of the original Swing map viewer,
     * where the tile zoom level is {@link #TILE_ZOOM_OFFSET} minus the stored level, so existing views carry over.
     * Can be called from any thread.
     * @return tile zoom level, {@link #MINIMUM_TILE_ZOOM} to {@link #MAXIMUM_TILE_ZOOM}
     */
    public int getTileZoom()
    {
        return Math.clamp(TILE_ZOOM_OFFSET - getZoom(), MINIMUM_TILE_ZOOM, MAXIMUM_TILE_ZOOM);
    }

    /**
     * Stores the zoom level of the map as an OpenStreetMap tile zoom level
     * @param tileZoom level, clamped to {@link #MINIMUM_TILE_ZOOM} to {@link #MAXIMUM_TILE_ZOOM}
     */
    public void setTileZoom(int tileZoom)
    {
        setZoom(TILE_ZOOM_OFFSET - Math.clamp(tileZoom, MINIMUM_TILE_ZOOM, MAXIMUM_TILE_ZOOM));
    }

    /**
     * Indicates if the map shows the radio reference trunked system sites near the user
     */
    public boolean isShowRadioReferenceSites()
    {
        return Setting.MAP_SHOW_RADIO_REFERENCE_SITES.getBoolean(DEFAULT_SHOW_RADIO_REFERENCE_SITES);
    }

    /**
     * Shows or hides the radio reference trunked system sites on the map
     */
    public void setShowRadioReferenceSites(boolean show)
    {
        if(show != isShowRadioReferenceSites())
        {
            Setting.MAP_SHOW_RADIO_REFERENCE_SITES.putBoolean(show);
            notifySettingsUpdated();
        }
    }

    /**
     * Identifier of the map tile server
     * @return identifier, or null when none was chosen
     */
    public String getTileServer()
    {
        return Setting.MAP_TILE_SERVER.get(null);
    }

    /**
     * Stores the identifier of the map tile server
     */
    public void setTileServer(String id)
    {
        if(id != null && !id.equals(getTileServer()))
        {
            Setting.MAP_TILE_SERVER.put(id);
            notifySettingsUpdated();
        }
    }

    /**
     * Style of the map tiles: following the theme, light or dark
     * @return style name, or null when none was chosen
     */
    public String getTileStyle()
    {
        return Setting.MAP_TILE_STYLE.get(null);
    }

    /**
     * Stores the style of the map tiles
     */
    public void setTileStyle(String style)
    {
        if(style != null && !style.equals(getTileStyle()))
        {
            Setting.MAP_TILE_STYLE.put(style);
            notifySettingsUpdated();
        }
    }

    /**
     * Key the user entered for a map tile server that needs one
     * @param id of the tile server
     * @return key, or null
     */
    public String getTileServerKey(String id)
    {
        return Setting.MAP_TILE_SERVER_KEY.with(id).get(null);
    }

    /**
     * Stores the key for a map tile server, or removes it when it's blank
     * @param id of the tile server
     * @param key for the server, or null
     */
    public void setTileServerKey(String id, String key)
    {
        String value = key != null && !key.isBlank() ? key.strip() : null;

        if(!Objects.equals(value, getTileServerKey(id)))
        {
            if(value != null)
            {
                Setting.MAP_TILE_SERVER_KEY.with(id).put(value);
            }
            else
            {
                Setting.MAP_TILE_SERVER_KEY.with(id).remove();
            }

            notifySettingsUpdated();
        }
    }
}
