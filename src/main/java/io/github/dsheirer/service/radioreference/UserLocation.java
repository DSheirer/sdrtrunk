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

/**
 * Location used to rank radio reference results by distance from the user.
 *
 * @param latitude in decimal degrees
 * @param longitude in decimal degrees
 * @param description of the location for display, e.g. "Syracuse, New York 13202" or the preferred county name
 * @param source of the location
 */
public record UserLocation(double latitude, double longitude, String description, Source source)
{
    /**
     * Where the location came from
     */
    public enum Source
    {
        /**
         * Set by the user, by hand or from a zip code lookup
         */
        USER,

        /**
         * Center of the user's preferred county, used when the user hasn't set a location.  Less accurate: a county
         * can be large.
         */
        PREFERRED_COUNTY
    }
}
