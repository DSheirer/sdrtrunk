/*
 * TileFactoryInfo.java
 *
 * Created on June 26, 2006, 10:47 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer;

import org.apache.commons.math3.util.FastMath;

import java.awt.geom.Point2D;

/**
 * A TileFactoryInfo encapsulates all information specific to a map server. This includes everything from the url to
 * load the map tiles from to the size and depth of the tiles. Theoretically any map server can be used by installing a
 * customized TileFactoryInfo. Currently
 * @author joshy
 */
public class TileFactoryInfo
{
	private int minimumZoomLevel;
	private int maximumZoomLevel;
	private int totalMapZoom;
	// the size of each tile (assumes they are square)
	private int tileSize = 256;

	/*
	 * The number of tiles wide at each zoom level
	 */
	private int[] mapWidthInTilesAtZoom;
	/**
	 * An array of coordinates in <em>pixels</em> that indicates the center in the world map for the given zoom level.
	 */
	private Point2D[] mapCenterInPixelsAtZoom;// = new Point2D.Double[18];

	/**
	 * An array of doubles that contain the number of pixels per degree of longitude at a give zoom level.
	 */
	private double[] longitudeDegreeWidthInPixels;

	/**
	 * An array of doubles that contain the number of radians per degree of longitude at a given zoom level (where
	 * longitudeRadianWidthInPixels[0] is the most zoomed out)
	 */
	private double[] longitudeRadianWidthInPixels;

	/**
	 * The base url for loading tiles from.
	 */
	protected String baseURL;
	private String xparam;
	private String yparam;
	private String zparam;
	private boolean xr2l = true;
	private boolean yt2b = true;

	private int defaultZoomLevel;

	/** A name for this info. */
	private String name;

	/**
	 * Creates a new instance of TileFactoryInfo. Note that TileFactoryInfo should be considered invariate, meaning that
	 * subclasses should ensure all of the properties stay the same after the class is constructed. Returning different
	 * values of getTileSize() for example is considered an error and may result in unexpected behavior.
	 * @param minimumZoomLevel The minimum zoom level
	 * @param maximumZoomLevel the maximum zoom level
	 * @param totalMapZoom the top zoom level, essentially the height of the pyramid
	 * @param tileSize the size of the tiles in pixels (must be square)
	 * @param xr2l if the x goes r to l (is this backwards?)
	 * @param yt2b if the y goes top to bottom
	 * @param baseURL the base url for grabbing tiles
	 * @param xparam the x parameter for the tile url
	 * @param yparam the y parameter for the tile url
	 * @param zparam the z parameter for the tile url
	 */
	/*
	 * @param xr2l true if tile x is measured from the far left of the map to the far right, or else false if based on
	 * the center line.
	 * @param yt2b true if tile y is measured from the top (north pole) to the bottom (south pole) or else false if
	 * based on the equator.
	 */
	public TileFactoryInfo(int minimumZoomLevel, int maximumZoomLevel, int totalMapZoom, int tileSize, boolean xr2l,
			boolean yt2b, String baseURL, String xparam, String yparam, String zparam)
	{
		this("name not provided", minimumZoomLevel, maximumZoomLevel, totalMapZoom, tileSize, xr2l, yt2b, baseURL,
				xparam, yparam, zparam);
	}

	/**
	 * Creates a new instance of TileFactoryInfo. Note that TileFactoryInfo should be considered invariate, meaning that
	 * subclasses should ensure all of the properties stay the same after the class is constructed. Returning different
	 * values of getTileSize() for example is considered an error and may result in unexpected behavior.
	 * @param name A name to identify this information.
	 * @param minimumZoomLevel The minimum zoom level
	 * @param maximumZoomLevel the maximum zoom level
	 * @param totalMapZoom the top zoom level, essentially the height of the pyramid
	 * @param tileSize the size of the tiles in pixels (must be square)
	 * @param xr2l if the x goes r to l (is this backwards?)
	 * @param yt2b if the y goes top to bottom
	 * @param baseURL the base url for grabbing tiles
	 * @param xparam the x parameter for the tile url
	 * @param yparam the y parameter for the tile url
	 * @param zparam the z parameter for the tile url
	 */
	/*
	 * @param xr2l true if tile x is measured from the far left of the map to the far right, or else false if based on
	 * the center line.
	 * @param yt2b true if tile y is measured from the top (north pole) to the bottom (south pole) or else false if
	 * based on the equator.
	 */
	public TileFactoryInfo(String name, int minimumZoomLevel, int maximumZoomLevel, int totalMapZoom, int tileSize,
			boolean xr2l, boolean yt2b, String baseURL, String xparam, String yparam, String zparam)
	{
		this.name = name;
		this.minimumZoomLevel = minimumZoomLevel;
		this.maximumZoomLevel = maximumZoomLevel;
		this.totalMapZoom = totalMapZoom;
		this.baseURL = baseURL;
		this.xparam = xparam;
		this.yparam = yparam;
		this.zparam = zparam;
		this.setXr2l(xr2l);
		this.setYt2b(yt2b);

		this.tileSize = tileSize;

		// init the num tiles wide
		int tilesize = this.getTileSize(0);

		longitudeDegreeWidthInPixels = new double[totalMapZoom + 1];
		longitudeRadianWidthInPixels = new double[totalMapZoom + 1];
		mapCenterInPixelsAtZoom = new Point2D.Double[totalMapZoom + 1];
		mapWidthInTilesAtZoom = new int[totalMapZoom + 1];

		// for each zoom level
		for (int z = totalMapZoom; z >= 0; --z)
		{
			// how wide is each degree of longitude in pixels
			longitudeDegreeWidthInPixels[z] = tilesize / 360.0;
			// how wide is each radian of longitude in pixels
			longitudeRadianWidthInPixels[z] = tilesize / (2.0 * FastMath.PI);
			int t2 = tilesize / 2;
			mapCenterInPixelsAtZoom[z] = new Point2D.Double(t2, t2);
			mapWidthInTilesAtZoom[z] = tilesize / this.getTileSize(0);
			tilesize *= 2;
		}

	}

	/**
	 * @return the minimum zoom level
	 */
	public int getMinimumZoomLevel()
	{
		return minimumZoomLevel;
	}

	// public void setMinimumZoomLevel(int minimumZoomLevel) {
	// this.minimumZoomLevel = minimumZoomLevel;
	// }

	/**
	 * @return the maximum zoom level
	 */
	public int getMaximumZoomLevel()
	{
		return maximumZoomLevel;
	}

	/**
	 * @return the total map zoom level
	 */
	public int getTotalMapZoom()
	{
		return totalMapZoom;
	}

	/**
	 * @param zoom the zoom level
	 * @return the map width in tiles
	 */
	public int getMapWidthInTilesAtZoom(int zoom)
	{
		return mapWidthInTilesAtZoom[zoom];
	}

	/**
	 * @param zoom the zoom level
	 * @return the map center in pixels
	 */
	public Point2D getMapCenterInPixelsAtZoom(int zoom)
	{
		return mapCenterInPixelsAtZoom[zoom];
	}

	/**
	 * Returns the tile url for the specified tile at the specified zoom level. By default it will generate a tile url
	 * using the base url and parameters specified in the constructor. Thus if <PRE><CODE>baseURl =
	 * http://www.myserver.com/maps?version=0.1 xparam = x yparam = y zparam = z tilepoint = [1,2] zoom level = 3
	 * </CODE> </PRE> then the resulting url would be:
	 * <pre><code>http://www.myserver.com/maps?version=0.1&amp;x=1&amp;y=2&amp;z=3</code></pre> Note that the URL can be
	 * a <CODE>file:</CODE> url.
	 * @param zoom the zoom level
	 * @param x the x value
	 * @param y the y value
	 * @return a valid url to load the tile
	 */

	public String getTileUrl(int x, int y, int zoom)
	{
		// System.out.println("getting tile at zoom: " + zoom);
		// System.out.println("map width at zoom = " + getMapWidthInTilesAtZoom(zoom));
		String ypart = "&" + yparam + "=" + y;
		// System.out.println("ypart = " + ypart);

		if (!yt2b)
		{
			int tilemax = getMapWidthInTilesAtZoom(zoom);
			// int y = tilePoint.getY();
			ypart = "&" + yparam + "=" + (tilemax / 2 - y - 1);
		}
		// System.out.println("new ypart = " + ypart);
		String url = baseURL + "&" + xparam + "=" + x + ypart +
		// "&" + yparam + "=" + tilePoint.getY() +
				"&" + zparam + "=" + zoom;
		return url;
	}

	/**
	 * Get the tile size.
	 * @param zoom the zoom level
	 * @return the tile size
	 */
	public int getTileSize(int zoom)
	{
		return tileSize;
	}

	/**
	 * @param zoom the zoom level
	 * @return the longitude degree width in pixels
	 */
	public double getLongitudeDegreeWidthInPixels(int zoom)
	{
		return longitudeDegreeWidthInPixels[zoom];
	}

	/**
	 * @param zoom the zoom level
	 * @return the longitude radian width in pixels
	 */
	public double getLongitudeRadianWidthInPixels(int zoom)
	{
		return longitudeRadianWidthInPixels[zoom];
	}

	/**
	 * @return indicates if the X coordinates of tiles go from right to left or left to right.
	 */
	public boolean isXr2l()
	{
		return xr2l;
	}

	/**
	 * @param xr2l indicates if the X coordinates of tiles go from right to left or left to right.
	 */
	public void setXr2l(boolean xr2l)
	{
		this.xr2l = xr2l;
	}

	/**
	 * @return indicates if the Y coordinates of tiles go from right to left or left to right.
	 */
	public boolean isYt2b()
	{
		return yt2b;
	}

	/**
	 * @param yt2b indicates if the Y coordinates of tiles go from right to left or left to right.
	 */
	public void setYt2b(boolean yt2b)
	{
		this.yt2b = yt2b;
	}

	/**
	 * @return the default zoom level
	 */
	public int getDefaultZoomLevel()
	{
		return defaultZoomLevel;
	}

	/**
	 * @param defaultZoomLevel the default zoom level
	 */
	public void setDefaultZoomLevel(int defaultZoomLevel)
	{
		this.defaultZoomLevel = defaultZoomLevel;
	}

	/**
	 * The name of this info.
	 * @return Returns the name of this info class for debugging or GUI widgets.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return the base URL
	 */
	public String getBaseURL()
	{
		return baseURL;
	}

	
}
