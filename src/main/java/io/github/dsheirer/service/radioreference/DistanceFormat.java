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

import java.util.Locale;

/**
 * Formats distances for display, in miles where the locale's country measures road distances in miles, otherwise in
 * kilometers.
 */
public final class DistanceFormat
{
    private static final double KILOMETERS_PER_MILE = 1.609344;
    private static final boolean MILES = usesMiles(Locale.getDefault());

    private DistanceFormat()
    {
    }

    /**
     * Distance for display in the default locale's unit, e.g. 12 mi or < 1 km
     */
    public static String format(double kilometers)
    {
        return format(kilometers, MILES);
    }

    /**
     * Distance for display, in miles or kilometers
     */
    public static String format(double kilometers, boolean miles)
    {
        double value = miles ? kilometers / KILOMETERS_PER_MILE : kilometers;
        String unit = miles ? " mi" : " km";
        return value < 1.0 ? "< 1" + unit : Math.round(value) + unit;
    }

    /**
     * Indicates if the locale's country measures road distances in miles
     */
    public static boolean usesMiles(Locale locale)
    {
        String country = locale.getCountry();
        return "US".equals(country) || "GB".equals(country) || "LR".equals(country) || "MM".equals(country);
    }
}
