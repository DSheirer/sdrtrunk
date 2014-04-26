/*
 * TileFactory.java
 *
 * Created on March 17, 2006, 8:01 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.jdesktop.swingx.mapviewer.util.GeoUtil;

/**
 * A class that can produce tiles and convert coordinates to pixels
 * @author joshy
 */
public abstract class TileFactory
{
	private TileFactoryInfo info;
	private List<TileListener> tileListeners = new ArrayList<TileListener>();

	/**
	 * Creates a new instance of TileFactory
	 * @param info a TileFactoryInfo to configure this TileFactory
	 */
	protected TileFactory(TileFactoryInfo info)
	{
		this.info = info;
	}

	/**
	 * Gets the size of an edge of a tile in pixels at the current zoom level. Tiles must be square.
	 * @param zoom the current zoom level
	 * @return the size of an edge of a tile in pixels
	 */
	public int getTileSize(int zoom)
	{
		return getInfo().getTileSize(zoom);
	}

	/**
	 * Returns a Dimension containing the width and height of the map, in tiles at the current zoom level. So a
	 * Dimension that returns 10x20 would be 10 tiles wide and 20 tiles tall. These values can be multipled by
	 * getTileSize() to determine the pixel width/height for the map at the given zoom level
	 * @return the size of the world bitmap in tiles
	 * @param zoom the current zoom level
	 */
	public Dimension getMapSize(int zoom)
	{
		return GeoUtil.getMapSize(zoom, getInfo());
	}

	/**
	 * Return the Tile at a given TilePoint and zoom level
	 * @return the tile that is located at the given tilePoint for this zoom level. For example, if getMapSize() returns
	 * 10x20 for this zoom, and the tilePoint is (3,5), then the appropriate tile will be located and returned. This
	 * method must not return null. However, it can return dummy tiles that contain no data if it wants. This is
	 * appropriate, for example, for tiles which are outside of the bounds of the map and if the factory doesn't
	 * implement wrapping.
	 * @param x the x value
	 * @param y the y value
	 * @param zoom the current zoom level
	 */
	public abstract Tile getTile(int x, int y, int zoom);

	/**
	 * Convert a pixel in the world bitmap at the specified zoom level into a GeoPosition
	 * @param pixelCoordinate a Point2D representing a pixel in the world bitmap
	 * @param zoom the zoom level of the world bitmap
	 * @return the converted GeoPosition
	 */
	public GeoPosition pixelToGeo(Point2D pixelCoordinate, int zoom)
	{
		return GeoUtil.getPosition(pixelCoordinate, zoom, getInfo());
	}

	/**
	 * Convert a GeoPosition to a pixel position in the world bitmap a the specified zoom level.
	 * @param c a GeoPosition
	 * @param zoomLevel the zoom level to extract the pixel coordinate for
	 * @return the pixel point
	 */
	public Point2D geoToPixel(GeoPosition c, int zoomLevel)
	{
		return GeoUtil.getBitmapCoordinate(c, zoomLevel, getInfo());
	}

	/**
	 * Get the TileFactoryInfo describing this TileFactory
	 * @return a TileFactoryInfo
	 */
	public TileFactoryInfo getInfo()
	{
		return info;
	}
	
	/**
	 * Adds a tile listener
	 * @param listener the listener
	 */
	public void addTileListener(TileListener listener)
	{
		tileListeners.add(listener);
	}
	
	/**
	 * Removes a tile listener
	 * @param listener the listener
	 */
	public void removeTileListener(TileListener listener)
	{
		tileListeners.remove(listener);
	}
	
	/**
	 * Disposes this TileFactory
	 */
	public abstract void dispose();
	
	/**
	 * Notifies all tile listeners
	 * @param tile the tile
	 */
	protected void fireTileLoadedEvent(Tile tile)
	{
		for (TileListener listener : tileListeners)
		{
			listener.tileLoaded(tile);
		}
	}

	/**
	 * Override this method to load the tile using, for example, an <code>ExecutorService</code>.
	 * @param tile The tile to load.
	 */
	protected abstract void startLoading(Tile tile);

}
