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
 * Web (spherical) Mercator projection used by OpenStreetMap tiles.  Positions are expressed as fractions of the world
 * width and height (0 to 1), independent of the zoom level: x grows eastward from -180 degrees longitude and y grows
 * southward from the top of the map at about 85.05 degrees north latitude.
 */
public final class Mercator
{
    /**
     * Pixel width and height of a map tile
     */
    public static final int TILE_SIZE = 256;

    /**
     * Northern and southern limit of the projection, where the map is square
     */
    public static final double MAX_LATITUDE = 85.05112878;

    private Mercator()
    {
    }

    /**
     * World width and height in pixels at the zoom level
     */
    public static double worldSize(int zoom)
    {
        return TILE_SIZE * (double)(1L << zoom);
    }

    /**
     * Number of tiles across the world at the zoom level
     */
    public static int tileCount(int zoom)
    {
        return 1 << zoom;
    }

    /**
     * Longitude to a fraction of the world width
     */
    public static double x(double longitude)
    {
        return (longitude + 180.0) / 360.0;
    }

    /**
     * Latitude to a fraction of the world height, clamped to the projection's limits
     */
    public static double y(double latitude)
    {
        double radians = Math.toRadians(Math.clamp(latitude, -MAX_LATITUDE, MAX_LATITUDE));
        return (1.0 - Math.log(Math.tan(radians) + 1.0 / Math.cos(radians)) / Math.PI) / 2.0;
    }

    /**
     * Fraction of the world width to a longitude, wrapped into -180 to 180 degrees
     */
    public static double longitude(double x)
    {
        return normalizeLongitude(x * 360.0 - 180.0);
    }

    /**
     * Fraction of the world height to a latitude
     */
    public static double latitude(double y)
    {
        double n = Math.PI * (1.0 - 2.0 * Math.clamp(y, 0.0, 1.0));
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    /**
     * Wraps a longitude into -180 to 180 degrees, since the map can be panned past +/-180 degrees
     */
    public static double normalizeLongitude(double longitude)
    {
        return ((longitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
    }
}
