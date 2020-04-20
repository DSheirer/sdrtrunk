/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package org.jdesktop.swingx.mapviewer.bmng;

import org.apache.commons.math3.util.FastMath;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.GeoPosition;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * @author joshy
 */
public class CylindricalProjectionTileFactory extends DefaultTileFactory
{
	/**
     * Uses {@link SLMapServerInfo}
     */
	public CylindricalProjectionTileFactory()
	{
		this(new SLMapServerInfo());
	}

	/**
	 * @param info the tile factory info
	 */
	public CylindricalProjectionTileFactory(SLMapServerInfo info)
	{
		super(info);
	}

	@Override
	public Dimension getMapSize(int zoom)
	{
		int midpoint = ((SLMapServerInfo) getInfo()).getMidpoint();
		if (zoom < midpoint)
		{
			int w = (int) FastMath.pow(2, midpoint - zoom);
			return new Dimension(w, w / 2);
			// return super.getMapSize(zoom);
		}
		return new Dimension(2, 1);
	}

	@Override
	public Point2D geoToPixel(GeoPosition c, int zoom)
	{
		// calc the pixels per degree
		Dimension mapSizeInTiles = getMapSize(zoom);
		// double size_in_tiles = (double)getInfo().getMapWidthInTilesAtZoom(zoom);
		// double size_in_tiles = Math.pow(2, getInfo().getTotalMapZoom() - zoom);
		double size_in_pixels = mapSizeInTiles.getWidth() * getInfo().getTileSize(zoom);
		double ppd = size_in_pixels / 360;

		// the center of the world
		double centerX = this.getTileSize(zoom) * mapSizeInTiles.getWidth() / 2;
		double centerY = this.getTileSize(zoom) * mapSizeInTiles.getHeight() / 2;

		double x = c.getLongitude() * ppd + centerX;
		double y = -c.getLatitude() * ppd + centerY;

		return new Point2D.Double(x, y);
	}

	@Override
	public GeoPosition pixelToGeo(Point2D pix, int zoom)
	{
		// calc the pixels per degree
		Dimension mapSizeInTiles = getMapSize(zoom);
		double size_in_pixels = mapSizeInTiles.getWidth() * getInfo().getTileSize(zoom);
		double ppd = size_in_pixels / 360;

		// the center of the world
		double centerX = this.getTileSize(zoom) * mapSizeInTiles.getWidth() / 2;
		double centerY = this.getTileSize(zoom) * mapSizeInTiles.getHeight() / 2;

		double lon = (pix.getX() - centerX) / ppd;
		double lat = -(pix.getY() - centerY) / ppd;

		return new GeoPosition(lat, lon);
	}

	/*
	 * x = lat * ppd + fact x - fact = lat * ppd (x - fact)/ppd = lat y = -lat*ppd + fact -(y-fact)/ppd = lat
	 */
}
