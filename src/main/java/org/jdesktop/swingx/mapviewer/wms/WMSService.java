/*
 * WMSService.java
 *
 * Created on October 7, 2006, 6:06 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer.wms;

import org.jdesktop.swingx.mapviewer.util.MercatorUtils;

/**
 * A class that represents a WMS mapping service. 
 * See http://en.wikipedia.org/wiki/Web_Map_Service for more information.
 * @author joshy
 */
public class WMSService
{
	private String baseUrl;
	private String layer;

	/** 
	 * Creates a new instance of WMSService
	 * TODO: not working -> remove 
	 */
	public WMSService()
	{
		// by default use a known nasa server
		setLayer("BMNG");
		setBaseUrl("http://wms.jpl.nasa.gov/wms.cgi?");
	}

	/**
	 * @param baseUrl the base URL
	 * @param layer the layer
	 */
	public WMSService(String baseUrl, String layer)
	{
		this.baseUrl = baseUrl;
		this.layer = layer;
	}

	/**
	 * Convertes to a WMS URL
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param zoom the zomm factor
	 * @param tileSize the tile size
	 * @return a URL request string 
	 */
	public String toWMSURL(int x, int y, int zoom, int tileSize)
	{
		String format = "image/jpeg";
		String styles = "";
		String srs = "EPSG:4326";
		int ts = tileSize;
		int circumference = widthOfWorldInPixels(zoom, tileSize);
		double radius = circumference / (2 * Math.PI);
		double ulx = MercatorUtils.xToLong(x * ts, radius);
		double uly = MercatorUtils.yToLat(y * ts, radius);
		double lrx = MercatorUtils.xToLong((x + 1) * ts, radius);
		double lry = MercatorUtils.yToLat((y + 1) * ts, radius);
		String bbox = ulx + "," + uly + "," + lrx + "," + lry;
		String url = getBaseUrl() + "version=1.1.1&request=" + "GetMap&Layers=" + layer + "&format=" + format
				+ "&BBOX=" + bbox + "&width=" + ts + "&height=" + ts + "&SRS=" + srs + "&Styles=" + styles +
				// "&transparent=TRUE"+
				"";
		return url;
	}

	private int widthOfWorldInPixels(int zoom, int TILE_SIZE)
	{
		// int TILE_SIZE = 256;
		int tiles = (int) Math.pow(2, zoom);
		int circumference = TILE_SIZE * tiles;
		return circumference;
	}

	/**
	 * @return the layer
	 */
	public String getLayer()
	{
		return layer;
	}

	/**
	 * @param layer the layer
	 */
	public void setLayer(String layer)
	{
		this.layer = layer;
	}

	/**
	 * @return the base URL
	 */
	public String getBaseUrl()
	{
		return baseUrl;
	}

	/**
	 * @param baseUrl the base URL
	 */
	public void setBaseUrl(String baseUrl)
	{
		this.baseUrl = baseUrl;
	}

}
