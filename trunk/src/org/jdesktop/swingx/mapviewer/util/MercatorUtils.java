/*
 * MercatorUtils.java
 *
 * Created on October 7, 2006, 6:02 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer.util;

/**
 * A utility class of methods that help when 
 * dealing with standard Mercator projections.
 * @author joshua.marinacci@sun.com
 */
public final class MercatorUtils
{

	/** 
	 * Creates a new instance of MercatorUtils 
	 */
	private MercatorUtils()
	{
	}

	/**
	 * @param longitudeDegrees the longitude in degrees
	 * @param radius the world radius in pixels
	 * @return the x value
	 */
	public static int longToX(double longitudeDegrees, double radius)
	{
		double longitude = Math.toRadians(longitudeDegrees);
		return (int) (radius * longitude);
	}

	/**
	 * @param latitudeDegrees the latitude in degrees
	 * @param radius the world radius in pixels
	 * @return the y value
	 */
	public static int latToY(double latitudeDegrees, double radius)
	{
		double latitude = Math.toRadians(latitudeDegrees);
		double y = radius / 2.0 * Math.log((1.0 + Math.sin(latitude)) / (1.0 - Math.sin(latitude)));
		return (int) y;
	}

	/**
	 * @param x the x value
	 * @param radius the world radius in pixels
	 * @return the longitude in degrees
	 */
	public static double xToLong(int x, double radius)
	{
		double longRadians = x / radius;
		double longDegrees = Math.toDegrees(longRadians);
		/*
		 * The user could have panned around the world a lot of times. Lat long goes from -180 to 180. So every time a
		 * user gets to 181 we want to subtract 360 degrees. Every time a user gets to -181 we want to add 360 degrees.
		 */
		int rotations = (int) Math.floor((longDegrees + 180) / 360);
		double longitude = longDegrees - (rotations * 360);
		return longitude;
	}

	/**
	 * @param y the y value
	 * @param radius the world radius in pixels
	 * @return the latitude in degrees
	 */
	public static double yToLat(int y, double radius)
	{
		double latitude = (Math.PI / 2) - (2 * Math.atan(Math.exp(-1.0 * y / radius)));
		return Math.toDegrees(latitude);
	}
}
