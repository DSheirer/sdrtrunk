/*
 * ESRITileFactory.java
 *
 * Created on November 7, 2006, 10:51 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer.esri;

import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import org.jdesktop.swingx.mapviewer.util.GeoUtil;

import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author rbair
 */
public class ESRITileFactory extends DefaultTileFactory
{
	private static final String projection = "8"; // mercator projection
	private static final String format = "png"; // get pngs back
	private String userId;
	private String datasource; // should be enum

	/** Creates a new instance of ESRITileFactory */
	public ESRITileFactory()
	{
		super(new ESRITileProviderInfo());
		((ESRITileProviderInfo) super.getInfo()).factory = this;
		datasource = "ArcWeb:TA.Streets.NA";
	}

	/**
	 * @param id the ESRI user id
	 */
	public void setUserID(String id)
	{
		this.userId = id;
	}

	private static final class ESRITileProviderInfo extends TileFactoryInfo
	{
		private ESRITileFactory factory;

		private ESRITileProviderInfo()
		{
			super(0, 17, 18, 256, false, true, "http://www.arcwebservices.com/services/v2006/restmap?actn=getMap", "","", "");
		}

		@Override
		public String getTileUrl(int x, int y, int zoom)
		{
			// "&usrid=&ds=&c=-117.1817|34.0556&sf=52500&fmt=&ocs=";
			// System.out.println("getting tile at zoom: " + zoom);
			// System.out.println("map width at zoom = " + getMapWidthInTilesAtZoom(zoom));

			// provide the center point of the tile, in lat/long coords
			int tileY = y;
			int tileX = x;
			int pixelX = tileX * factory.getTileSize(zoom) + (factory.getTileSize(zoom) / 2);
			int pixelY = tileY * factory.getTileSize(zoom) + (factory.getTileSize(zoom) / 2);

			GeoPosition latlong = GeoUtil.getPosition(new Point2D.Double(pixelX, pixelY), zoom, this);

			// Chris is going to hate me for this (relying on 72dpi!), but:
			// 72 pixels per inch. The earth is 24,859.82 miles in circumference, at the equator.
			// Thus, the earth is 24,859.82 * 5280 * 12 * 72 pixels in circumference.

			double numFeetPerDegreeLong = 24859.82 * 5280 / 360; // the number of feet per degree longitude at the
																	// equator
			double numPixelsPerDegreeLong = getLongitudeDegreeWidthInPixels(zoom);
			double numPixelsPerFoot = 96 * 12;
			int sf = (int) (numFeetPerDegreeLong / (numPixelsPerDegreeLong / numPixelsPerFoot));

			// round lat and long to 5 decimal places
			BigDecimal lat = new BigDecimal(latlong.getLatitude());
			BigDecimal lon = new BigDecimal(latlong.getLongitude());
			lat = lat.setScale(5, RoundingMode.DOWN);
			lon = lon.setScale(5, RoundingMode.DOWN);
			System.out.println("Tile      : [" + tileX + ", " + tileY + "]");
			System.out.println("Pixel     : [" + pixelX + ", " + pixelY + "]");
			System.out.println("Lat/Long  : [" + latlong.getLatitude() + ", " + latlong.getLongitude() + "]");
			System.out.println("Lat2/Long2: [" + lat.doubleValue() + ", " + lon.doubleValue() + "]");

			String url = baseURL + "&usrid=" + factory.userId + "&ds=" + factory.datasource + "&c=" + lon.doubleValue()
					+ "%7C" + lat.doubleValue() + "&sf=" + sf + // 52500" +
					"&fmt=" + format + "&ocs=" + projection;
			System.out.println("the URL: " + url);
			return url;
		}
	}
}
