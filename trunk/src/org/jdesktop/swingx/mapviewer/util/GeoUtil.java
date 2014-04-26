/*
 * GeoUtil.java
 *
 * Created on June 26, 2006, 10:44 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer.util;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;

import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoBounds;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;

/**
 * These are math utilities for converting between pixels, tiles, and geographic coordinates. Implements a Google Maps
 * style mercator projection.
 * @author joshy
 */
public final class GeoUtil
{
	/**
	 * @param zoom the zoom level
	 * @param info the tile factory info
	 * @return the size of the map at the given zoom, in tiles (num tiles tall by num tiles wide)
	 */
	public static Dimension getMapSize(int zoom, TileFactoryInfo info)
	{
		return new Dimension(info.getMapWidthInTilesAtZoom(zoom), info.getMapWidthInTilesAtZoom(zoom));
	}

	/**
	 * @param x the x value 
	 * @param y the y value
	 * @param zoomLevel the zoom level
	 * @param info the tile factory info
	 * @return true if this point in <em>tiles</em> is valid at this zoom level. For example, if the zoom level is 0
	 * (zoomed all the way out, where there is only one tile), then x,y must be 0,0
	 */
	public static boolean isValidTile(int x, int y, int zoomLevel, TileFactoryInfo info)
	{
		// int x = (int)coord.getX();
		// int y = (int)coord.getY();
		// if off the map to the top or left
		if (x < 0 || y < 0)
		{
			return false;
		}
		// if of the map to the right
		if (info.getMapCenterInPixelsAtZoom(zoomLevel).getX() * 2 <= x * info.getTileSize(zoomLevel))
		{
			return false;
		}
		// if off the map to the bottom
		if (info.getMapCenterInPixelsAtZoom(zoomLevel).getY() * 2 <= y * info.getTileSize(zoomLevel))
		{
			return false;
		}
		// if out of zoom bounds
		if (zoomLevel < info.getMinimumZoomLevel() || zoomLevel > info.getMaximumZoomLevel())
		{
			return false;
		}
		return true;
	}

	/**
	 * Given a position (latitude/longitude pair) and a zoom level, return the appropriate point in <em>pixels</em>. The
	 * zoom level is necessary because pixel coordinates are in terms of the zoom level
	 * @param c A lat/lon pair
	 * @param zoomLevel the zoom level to extract the pixel coordinate for
	 * @param info the tile factory info
	 * @return the coordinate
	 */
	public static Point2D getBitmapCoordinate(GeoPosition c, int zoomLevel, TileFactoryInfo info)
	{
		return getBitmapCoordinate(c.getLatitude(), c.getLongitude(), zoomLevel, info);
	}

	/**
	 * Given a position (latitude/longitude pair) and a zoom level, return the appropriate point in <em>pixels</em>. The
	 * zoom level is necessary because pixel coordinates are in terms of the zoom level
	 * @param latitude the latitude
	 * @param longitude the longitude
	 * @param zoomLevel the zoom level to extract the pixel coordinate for
	 * @param info the tile factory info
	 * @return the coordinate
	 */
	public static Point2D getBitmapCoordinate(double latitude, double longitude, int zoomLevel, TileFactoryInfo info)
	{
		double x = info.getMapCenterInPixelsAtZoom(zoomLevel).getX() + longitude
				* info.getLongitudeDegreeWidthInPixels(zoomLevel);
		double e = Math.sin(latitude * (Math.PI / 180.0));
		if (e > 0.9999)
		{
			e = 0.9999;
		}
		if (e < -0.9999)
		{
			e = -0.9999;
		}
		double y = info.getMapCenterInPixelsAtZoom(zoomLevel).getY() + 0.5 * Math.log((1 + e) / (1 - e)) * -1
				* (info.getLongitudeRadianWidthInPixels(zoomLevel));
		return new Point2D.Double(x, y);
	}

	/** 
	 * Convert an on screen pixel coordinate and a zoom level to a geo position
	 * @param pixelCoordinate the coordinate in pixels
	 * @param zoom the zoom level
	 * @param info the tile factory info
	 * @return a geo position
	 */
	public static GeoPosition getPosition(Point2D pixelCoordinate, int zoom, TileFactoryInfo info)
	{
		// p(" --bitmap to latlon : " + coord + " " + zoom);
		double wx = pixelCoordinate.getX();
		double wy = pixelCoordinate.getY();
		// this reverses getBitmapCoordinates
		double flon = (wx - info.getMapCenterInPixelsAtZoom(zoom).getX()) / info.getLongitudeDegreeWidthInPixels(zoom);
		double e1 = (wy - info.getMapCenterInPixelsAtZoom(zoom).getY())
				/ (-1 * info.getLongitudeRadianWidthInPixels(zoom));
		double e2 = (2 * Math.atan(Math.exp(e1)) - Math.PI / 2) / (Math.PI / 180.0);
		double flat = e2;
		GeoPosition wc = new GeoPosition(flat, flon);
		return wc;
	}

	/**
	 * Gets the map bounds.
	 * @param mapViewer The map viewer.
	 * @return Returns the bounds.
	 */
	public static GeoBounds getMapBounds(JXMapViewer mapViewer)
	{
		return new GeoBounds(getMapGeoBounds(mapViewer));
	}

	/**
	 * Gets the bounds as a set of two <code>GeoPosition</code> objects.
	 * @param mapViewer The map viewer.
	 * @return Returns the set of two <code>GeoPosition</code> objects that represent the north west and south east
	 * corners of the map.
	 */
	private static Set<GeoPosition> getMapGeoBounds(JXMapViewer mapViewer)
	{
		Set<GeoPosition> set = new HashSet<GeoPosition>();
		TileFactory tileFactory = mapViewer.getTileFactory();
		int zoom = mapViewer.getZoom();
		Rectangle2D bounds = mapViewer.getViewportBounds();
		Point2D pt = new Point2D.Double(bounds.getX(), bounds.getY());
		set.add(tileFactory.pixelToGeo(pt, zoom));
		pt = new Point2D.Double(bounds.getX() + bounds.getWidth(), bounds.getY() + bounds.getHeight());
		set.add(tileFactory.pixelToGeo(pt, zoom));
		return set;
	}

}
