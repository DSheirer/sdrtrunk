/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.service.radioreference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Approximate bounding boxes of the United States states and Canadian provinces and territories, used to find the
 * states that may have counties near a location.  The radio reference web service can't search by location and its
 * states have no coordinates, so this table picks the candidate states, and the radio reference county centers then
 * decide which counties are near.  The boxes only need to be close: candidates are found with a margin added.
 *
 * @param countryCode ISO country code
 * @param stateCode postal abbreviation
 * @param minimumLatitude southern edge in degrees
 * @param maximumLatitude northern edge in degrees
 * @param minimumLongitude western edge in degrees
 * @param maximumLongitude eastern edge in degrees
 */
public record StateBounds(String countryCode, String stateCode, double minimumLatitude, double maximumLatitude,
                          double minimumLongitude, double maximumLongitude)
{
    //Extra margin in degrees for the approximate boxes
    private static final double MARGIN_DEGREES = 0.25;
    private static final double KILOMETERS_PER_DEGREE = 111.32;

    private static final List<StateBounds> STATES = List.of(
        us("AL", 30.14, 35.01, -88.47, -84.89), us("AK", 51.21, 71.39, -179.99, -129.99),
        us("AZ", 31.33, 37.00, -114.82, -109.04), us("AR", 33.00, 36.50, -94.62, -89.64),
        us("CA", 32.53, 42.01, -124.41, -114.13), us("CO", 36.99, 41.00, -109.06, -102.04),
        us("CT", 40.98, 42.05, -73.73, -71.79), us("DE", 38.45, 39.84, -75.79, -75.05),
        us("DC", 38.79, 38.99, -77.12, -76.91), us("FL", 24.40, 31.00, -87.63, -80.03),
        us("GA", 30.36, 35.00, -85.61, -80.84), us("HI", 18.91, 28.40, -178.33, -154.81),
        us("ID", 41.99, 49.00, -117.24, -111.04), us("IL", 36.97, 42.51, -91.51, -87.50),
        us("IN", 37.77, 41.76, -88.10, -84.78), us("IA", 40.38, 43.50, -96.64, -90.14),
        us("KS", 36.99, 40.00, -102.05, -94.59), us("KY", 36.50, 39.15, -89.57, -81.96),
        us("LA", 28.93, 33.02, -94.04, -88.82), us("ME", 43.06, 47.46, -71.08, -66.95),
        us("MD", 37.91, 39.72, -79.49, -75.05), us("MA", 41.24, 42.89, -73.51, -69.93),
        us("MI", 41.70, 48.31, -90.42, -82.41), us("MN", 43.50, 49.38, -97.24, -89.49),
        us("MS", 30.17, 35.00, -91.66, -88.10), us("MO", 35.99, 40.61, -95.77, -89.10),
        us("MT", 44.36, 49.00, -116.05, -104.04), us("NE", 40.00, 43.00, -104.05, -95.31),
        us("NV", 35.00, 42.00, -120.01, -114.04), us("NH", 42.70, 45.31, -72.56, -70.61),
        us("NJ", 38.93, 41.36, -75.56, -73.89), us("NM", 31.33, 37.00, -109.05, -103.00),
        us("NY", 40.50, 45.02, -79.76, -71.86), us("NC", 33.84, 36.59, -84.32, -75.46),
        us("ND", 45.94, 49.00, -104.05, -96.55), us("OH", 38.40, 41.98, -84.82, -80.52),
        us("OK", 33.62, 37.00, -103.00, -94.43), us("OR", 41.99, 46.29, -124.57, -116.46),
        us("PA", 39.72, 42.27, -80.52, -74.69), us("RI", 41.15, 42.02, -71.86, -71.12),
        us("SC", 32.03, 35.22, -83.35, -78.54), us("SD", 42.48, 45.95, -104.06, -96.44),
        us("TN", 34.98, 36.68, -90.31, -81.65), us("TX", 25.84, 36.50, -106.65, -93.51),
        us("UT", 37.00, 42.00, -114.05, -109.04), us("VT", 42.73, 45.02, -73.44, -71.46),
        us("VA", 36.54, 39.47, -83.68, -75.24), us("WA", 45.54, 49.00, -124.76, -116.92),
        us("WV", 37.20, 40.64, -82.64, -77.72), us("WI", 42.49, 47.08, -92.89, -86.81),
        us("WY", 40.99, 45.01, -111.06, -104.05), us("PR", 17.88, 18.52, -67.95, -65.22),
        ca("AB", 48.99, 60.00, -120.00, -110.00), ca("BC", 48.30, 60.00, -139.06, -114.03),
        ca("MB", 48.99, 60.00, -102.03, -88.94), ca("NB", 44.60, 48.07, -69.06, -63.77),
        ca("NL", 46.61, 60.37, -67.80, -52.62), ca("NS", 43.37, 47.04, -66.43, -59.68),
        ca("NT", 60.00, 78.76, -136.45, -101.98), ca("NU", 51.65, 83.11, -120.68, -61.19),
        ca("ON", 41.68, 56.86, -95.16, -74.32), ca("PE", 45.95, 47.07, -64.42, -61.97),
        ca("QC", 44.99, 62.59, -79.76, -57.10), ca("SK", 49.00, 60.00, -110.00, -101.36),
        ca("YT", 60.00, 69.65, -141.00, -123.81));

    private static StateBounds us(String code, double minimumLatitude, double maximumLatitude,
                                  double minimumLongitude, double maximumLongitude)
    {
        return new StateBounds("US", code, minimumLatitude, maximumLatitude, minimumLongitude, maximumLongitude);
    }

    private static StateBounds ca(String code, double minimumLatitude, double maximumLatitude,
                                  double minimumLongitude, double maximumLongitude)
    {
        return new StateBounds("CA", code, minimumLatitude, maximumLatitude, minimumLongitude, maximumLongitude);
    }

    /**
     * Country and state code, e.g. US-NY
     */
    public String key()
    {
        return countryCode + "-" + stateCode;
    }

    /**
     * Approximate distance in degrees from the location to the box, or 0 when the location is inside
     */
    private double distanceDegrees(double latitude, double longitude)
    {
        double latitudeDistance = Math.max(0, Math.max(minimumLatitude - latitude, latitude - maximumLatitude));
        double longitudeDistance = Math.max(0, Math.max(minimumLongitude - longitude, longitude - maximumLongitude));
        return Math.hypot(latitudeDistance, longitudeDistance * Math.cos(Math.toRadians(latitude)));
    }

    /**
     * States whose boxes are within the radius of the location, plus a margin, nearest first
     * @param latitude of the location
     * @param longitude of the location
     * @param radiusKilometers around the location
     * @return states, or an empty list when the location isn't near the United States or Canada
     */
    public static List<StateBounds> near(double latitude, double longitude, double radiusKilometers)
    {
        double margin = (radiusKilometers / KILOMETERS_PER_DEGREE) + MARGIN_DEGREES;
        List<StateBounds> near = new ArrayList<>();

        for(StateBounds state: STATES)
        {
            if(state.distanceDegrees(latitude, longitude) <= margin)
            {
                near.add(state);
            }
        }

        near.sort(Comparator.comparingDouble(state -> state.distanceDegrees(latitude, longitude)));
        return near;
    }
}
