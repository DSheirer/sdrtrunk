/*
 * *****************************************************************************
 * Copyright (C) 2014-2021 Dennis Sheirer
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

package io.github.dsheirer.identifier.location;

import java.text.DecimalFormat;

/**
 * Geospatial point (location)
 */
public class Point
{
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00000");

    private double mLatitude;
    private double mLongitude;

    /**
     * Constructs an instance
     * @param latitude in decimal degrees
     * @param longitude in decimal degrees
     */
    public Point(double latitude, double longitude)
    {
        mLatitude = latitude;
        mLongitude = longitude;
    }

    public double getLatitude()
    {
        return mLatitude;
    }

    public double getLongitude()
    {
        return mLongitude;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(DECIMAL_FORMAT.format(Math.abs(mLatitude))).append(mLatitude >= 0 ? "N " : "S ");
        sb.append(DECIMAL_FORMAT.format(Math.abs(mLongitude))).append(mLongitude >= 0 ? "E" : "W");
        return sb.toString();
    }
}
