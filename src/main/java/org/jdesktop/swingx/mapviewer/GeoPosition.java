/*
 * GeoPosition.java
 *
 * Created on March 31, 2006, 9:15 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer;

/**
 * An immutable coordinate in the real (geographic) world, 
 * composed of a latitude and a longitude.
 * @author rbair
 */
public class GeoPosition {
    private double latitude;
    private double longitude;
    
    /**
     * Creates a new instance of GeoPosition from the specified
     * latitude and longitude. These are double values in decimal degrees, not
     * degrees, minutes, and seconds.  Use the other constructor for those.
     * @param latitude a latitude value in decmial degrees
     * @param longitude a longitude value in decimal degrees
     */
    public GeoPosition(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
    // must be an array of length two containing lat then long in that order.
    /**
     * Creates a new instance of GeoPosition from the specified
     * latitude and longitude as an array of two doubles, with the
     * latitude first. These are double values in decimal degrees, not
     * degrees, minutes, and seconds.  Use the other constructor for those.
     * @param coords latitude and longitude as a double array of length two
     */
    public GeoPosition(double [] coords) {
        this.latitude = coords[0];
        this.longitude = coords[1];
    }
    
    /**
     * Creates a new instance of GeoPosition from the specified
     * latitude and longitude. 
     * Each are specified as degrees, minutes, and seconds; not
     * as decimal degrees. Use the other constructor for those.
     * @param latDegrees the degrees part of the current latitude
     * @param latMinutes the minutes part of the current latitude
     * @param latSeconds the seconds part of the current latitude
     * @param lonDegrees the degrees part of the current longitude
     * @param lonMinutes the minutes part of the current longitude
     * @param lonSeconds the seconds part of the current longitude
     */
    public GeoPosition(int latDegrees, int latMinutes, int latSeconds,
    		int lonDegrees, int lonMinutes, int lonSeconds) {
        this(latDegrees + (latMinutes + latSeconds/60.0)/60.0,
             lonDegrees + (lonMinutes + lonSeconds/60.0)/60.0);
    }
    
    /**
     * Get the latitude as decimal degrees
     * @return the latitude as decimal degrees
     */
    public double getLatitude() {
        return latitude;
    }
    
    /**
     * Get the longitude as decimal degrees
     * @return the longitude as decimal degrees
     */
    public double getLongitude() {
        return longitude;
    }
    
    @Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(latitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(longitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
    
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof GeoPosition))
			return false;
		GeoPosition other = (GeoPosition) obj;
		if (Double.doubleToLongBits(latitude) != Double.doubleToLongBits(other.latitude))
			return false;
		if (Double.doubleToLongBits(longitude) != Double.doubleToLongBits(other.longitude))
			return false;
		return true;
	}
    
    @Override
	public String toString() {
        return "[" + latitude + ", " + longitude + "]";
    }
}