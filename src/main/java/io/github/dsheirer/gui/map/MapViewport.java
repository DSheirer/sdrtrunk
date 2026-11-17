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

import io.github.dsheirer.map.GeoPosition;
import io.github.dsheirer.preference.map.MapPreference;

/**
 * Part of the world in view: the center point, the tile zoom level and the size of the view in pixels, with the
 * conversions between positions and screen pixels.  The map repeats east and west, so it can be panned around the
 * world, and it stops at the top and bottom of the projection.  Not thread safe: use it on the JavaFX thread.
 */
public class MapViewport
{
    private double mCenterX = Mercator.x(0);
    private double mCenterY = Mercator.y(0);
    private int mZoom = 2;
    private double mWidth;
    private double mHeight;

    /**
     * Constructs an instance
     */
    public MapViewport()
    {
    }

    /**
     * Tile zoom level
     */
    public int getZoom()
    {
        return mZoom;
    }

    /**
     * Sets the tile zoom level, keeping the center point
     * @param zoom level, clamped to the map preference's range
     */
    public void setZoom(int zoom)
    {
        mZoom = Math.clamp(zoom, MapPreference.MINIMUM_TILE_ZOOM, MapPreference.MAXIMUM_TILE_ZOOM);
        clamp();
    }

    /**
     * Changes the zoom level, keeping the position under the screen point in place
     * @param zoom level, clamped to the map preference's range
     * @param x of the screen point
     * @param y of the screen point
     */
    public void zoomAt(int zoom, double x, double y)
    {
        double worldX = (getLeft() + x) / getWorldSize();
        double worldY = (getTop() + y) / getWorldSize();
        mZoom = Math.clamp(zoom, MapPreference.MINIMUM_TILE_ZOOM, MapPreference.MAXIMUM_TILE_ZOOM);
        mCenterX = worldX - (x - mWidth / 2.0) / getWorldSize();
        mCenterY = worldY - (y - mHeight / 2.0) / getWorldSize();
        clamp();
    }

    public double getWidth()
    {
        return mWidth;
    }

    public double getHeight()
    {
        return mHeight;
    }

    /**
     * Sets the size of the view, keeping the center point
     */
    public void setSize(double width, double height)
    {
        mWidth = Math.max(0, width);
        mHeight = Math.max(0, height);
        clamp();
    }

    /**
     * Center point of the view
     */
    public GeoPosition getCenter()
    {
        return new GeoPosition(Mercator.latitude(mCenterY), Mercator.longitude(mCenterX));
    }

    /**
     * Centers the view on the position
     */
    public void setCenter(GeoPosition position)
    {
        mCenterX = Mercator.x(position.getLongitude());
        mCenterY = Mercator.y(position.getLatitude());
        clamp();
    }

    /**
     * Moves the view by screen pixels, as when the map is dragged by the opposite amount
     * @param dx pixels to move the view east
     * @param dy pixels to move the view south
     */
    public void pan(double dx, double dy)
    {
        mCenterX += dx / getWorldSize();
        mCenterY += dy / getWorldSize();
        clamp();
    }

    /**
     * World width and height in pixels at the current zoom level
     */
    public double getWorldSize()
    {
        return Mercator.worldSize(mZoom);
    }

    /**
     * World pixel at the left edge of the view, which is whole so tiles line up with screen pixels.  It can be
     * negative or past the world width, since the map repeats east and west.
     */
    public double getLeft()
    {
        return Math.round(mCenterX * getWorldSize() - mWidth / 2.0);
    }

    /**
     * World pixel at the top edge of the view, which is whole so tiles line up with screen pixels
     */
    public double getTop()
    {
        return Math.round(mCenterY * getWorldSize() - mHeight / 2.0);
    }

    /**
     * Screen x of the longitude, for the copy of the world nearest the middle of the view
     */
    public double getScreenX(double longitude)
    {
        double worldSize = getWorldSize();
        double x = Mercator.x(longitude) * worldSize - getLeft();
        double middle = mWidth / 2.0;
        return x - Math.round((x - middle) / worldSize) * worldSize;
    }

    /**
     * Screen y of the latitude
     */
    public double getScreenY(double latitude)
    {
        return Mercator.y(latitude) * getWorldSize() - getTop();
    }

    /**
     * Position at the screen point
     */
    public GeoPosition getPosition(double x, double y)
    {
        double worldSize = getWorldSize();
        return new GeoPosition(Mercator.latitude((getTop() + y) / worldSize),
            Mercator.longitude((getLeft() + x) / worldSize));
    }

    /**
     * Keeps the center within one copy of the world, and the top and bottom of the map at the edges of the view, or
     * the map in the middle when it's shorter than the view
     */
    private void clamp()
    {
        mCenterX = mCenterX - Math.floor(mCenterX);

        double worldSize = getWorldSize();
        double halfHeight = mHeight / 2.0 / worldSize;

        if(halfHeight >= 0.5)
        {
            mCenterY = 0.5;
        }
        else
        {
            mCenterY = Math.clamp(mCenterY, halfHeight, 1.0 - halfHeight);
        }
    }
}
