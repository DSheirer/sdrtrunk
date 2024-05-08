/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
 *
 * @author rbair
 */
public class GeoPosition
{
    private double latitude;
    private double longitude;

    /**
     * Creates a new instance of GeoPosition from the specified
     * latitude and longitude. These are double values in decimal degrees, not
     * degrees, minutes, and seconds.  Use the other constructor for those.
     *
     * @param latitude a latitude value in decmial degrees
     * @param longitude a longitude value in decimal degrees
     */
    public GeoPosition(double latitude, double longitude)
    {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Creates a new instance of GeoPosition from the specified
     * latitude and longitude as an array of two doubles, with the
     * latitude first. These are double values in decimal degrees, not
     * degrees, minutes, and seconds.  Use the other constructor for those.
     *
     * @param coords latitude and longitude as a double array of length two
     */
    public GeoPosition(double[] coords)
    {
        this.latitude = coords[0];
        this.longitude = coords[1];
    }

    /**
     * Creates a new instance of GeoPosition from the specified
     * latitude and longitude.
     * Each are specified as degrees, minutes, and seconds; not
     * as decimal degrees. Use the other constructor for those.
     *
     * @param latDegrees the degrees part of the current latitude
     * @param latMinutes the minutes part of the current latitude
     * @param latSeconds the seconds part of the current latitude
     * @param lonDegrees the degrees part of the current longitude
     * @param lonMinutes the minutes part of the current longitude
     * @param lonSeconds the seconds part of the current longitude
     */
    public GeoPosition(int latDegrees, int latMinutes, int latSeconds,
                       int lonDegrees, int lonMinutes, int lonSeconds)
    {
        this(latDegrees + (latMinutes + latSeconds / 60.0) / 60.0,
            lonDegrees + (lonMinutes + lonSeconds / 60.0) / 60.0);
    }

    /**
     * Get the latitude as decimal degrees
     *
     * @return the latitude as decimal degrees
     */
    public double getLatitude()
    {
        return latitude;
    }

    /**
     * Get the longitude as decimal degrees
     *
     * @return the longitude as decimal degrees
     */
    public double getLongitude()
    {
        return longitude;
    }

    /**
     * Formats the position as Degrees Minutes Seconds (DMS)
     */
    public String toDMS()
    {
        int latDegrees = (int)Math.floor(Math.abs(latitude));
        double latMinutes = (Math.abs(latitude) - latDegrees) * 60.0;
        int latMinutesInt = (int)Math.floor(latMinutes);
        double latSeconds = (latMinutes - latMinutesInt) * 60.0;
        int latSecondsInt = (int)latSeconds;

        int lonDegrees = (int)Math.floor(Math.abs(longitude));
        double lonMinutes = (Math.abs(longitude) - lonDegrees) * 60.0;
        int lonMinutesInt = (int)Math.floor(lonMinutes);
        double lonSeconds = (lonMinutes - lonMinutesInt) * 60.0;
        int lonSecondsInt = (int)lonSeconds;

        StringBuilder sb = new StringBuilder();
        sb.append(latDegrees).append("°");
        sb.append(latMinutesInt).append("'");
        sb.append(latSecondsInt).append("");
        sb.append(latitude >= 0 ? "N" : "S");
        sb.append(", ");
        sb.append(lonDegrees).append("°");
        sb.append(lonMinutesInt).append("'");
        sb.append(lonSecondsInt).append("");
        sb.append(longitude >= 0 ? "E" : "W");
        return sb.toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(latitude);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
        {
            return true;
        }
        if(obj == null)
        {
            return false;
        }
        if(!(obj instanceof GeoPosition))
        {
            return false;
        }
        GeoPosition other = (GeoPosition)obj;
        if(Double.doubleToLongBits(latitude) != Double.doubleToLongBits(other.latitude))
        {
            return false;
        }
        if(Double.doubleToLongBits(longitude) != Double.doubleToLongBits(other.longitude))
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "[" + latitude + ", " + longitude + "]";
    }

    public boolean isValid()
    {
        return -90.0 <= latitude && latitude <= 90.0 && -180.0 <= longitude && longitude <= 180.0;
    }
}