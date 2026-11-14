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

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Radio reference frequency near the user, a candidate for a signal at that frequency.  Stored in the frequency index
 * file, so changing the fields needs a new index file version.
 *
 * @param frequencyHz downlink frequency in Hertz
 * @param title of the frequency: the description or alpha tag of a conventional frequency, or the system name of a
 * trunked site frequency
 * @param alphaTag of a conventional frequency, or null
 * @param mode of a conventional frequency (e.g. FMN) or the system type of a trunked site frequency, or null
 * @param tone of a conventional frequency as radio reference lists it, or null
 * @param use of a trunked site frequency (e.g. Control), or null
 * @param encrypted true if radio reference lists the frequency as encrypted
 * @param source where the frequency is listed: county or agency, category and subcategory, or system site
 * @param latitude of the frequency's coverage (subcategory, county, agency or site), or null when unknown
 * @param longitude of the frequency's coverage, or null when unknown
 * @param systemId radio reference id of the trunked system of a site frequency, or null for a conventional frequency
 * @param siteId radio reference id of the site of a site frequency, or null for a conventional frequency
 */
public record FrequencyCandidate(long frequencyHz, String title, String alphaTag, String mode, String tone, String use,
                                 boolean encrypted, String source, Double latitude, Double longitude, Integer systemId,
                                 Integer siteId)
{
    /**
     * Indicates if this is a trunked system site frequency.  Not stored in the index file.
     */
    @JsonIgnore
    public boolean isSiteFrequency()
    {
        return systemId != null && siteId != null;
    }

    /**
     * Indicates if the candidate has a location
     */
    public boolean hasLocation()
    {
        return latitude != null && longitude != null;
    }
}
