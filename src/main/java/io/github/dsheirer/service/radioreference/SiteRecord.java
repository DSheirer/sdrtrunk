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

import java.util.List;

/**
 * Trunked radio system site near the user, from the radio reference frequency index.  Stored in the index file.
 *
 * @param systemId radio reference system id
 * @param systemName of the trunked system
 * @param protocol of the system: the radio reference system type and flavor, e.g. Project 25 Phase II, or null
 * @param voice of the system, e.g. APCO-25 Common Air Interface Exclusive, or null
 * @param siteId radio reference site id
 * @param siteNumber of the site in the system
 * @param description of the site, or null
 * @param county where the site is, or null
 * @param latitude of the site
 * @param longitude of the site
 * @param rangeKilometers of the site's coverage, or 0 when unknown
 * @param nac P25 network access code, or null
 * @param rfss P25 RF subsystem, or 0 when not listed
 * @param zone of the site, or 0 when not listed
 * @param channels of the site
 */
public record SiteRecord(int systemId, String systemName, String protocol, String voice, int siteId, int siteNumber,
                         String description, String county, double latitude, double longitude, double rangeKilometers,
                         String nac, int rfss, int zone, List<SiteChannel> channels)
{
    /**
     * Site frequency
     * @param frequencyHz downlink frequency in Hertz
     * @param logicalChannelNumber of the frequency, or 0 when not listed
     * @param use of the frequency, e.g. Control, or null for a voice channel
     */
    public record SiteChannel(long frequencyHz, int logicalChannelNumber, String use)
    {
    }
}
