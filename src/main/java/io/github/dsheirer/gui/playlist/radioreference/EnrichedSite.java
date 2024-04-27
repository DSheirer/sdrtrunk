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

package io.github.dsheirer.gui.playlist.radioreference;

import io.github.dsheirer.rrapi.type.CountyInfo;
import io.github.dsheirer.rrapi.type.Site;

/**
 * Wrapper class to join a Site and a corresponding County Info
 */
public class EnrichedSite implements Comparable<EnrichedSite>
{
    private static final String PHASE_2_TDMA_MODULATION = "TDMA";
    private Site mSite;
    private CountyInfo mCountyInfo;

    /**
     * Constructs an instance
     *
     * @param site object
     * @param countyInfo optional
     */
    public EnrichedSite(Site site, CountyInfo countyInfo)
    {
        mSite = site;
        mCountyInfo = countyInfo;
    }

    public static String format(int value)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(value).append(" (").append(Integer.toHexString(value).toUpperCase()).append(")");
        return sb.toString();
    }

    /**
     * Site instance
     *
     * @return site or null
     */
    public Site getSite()
    {
        return mSite;
    }

    /**
     * Sets the site instance
     *
     * @param site or null
     */
    public void setSite(Site site)
    {
        mSite = site;
    }

    /**
     * County information
     *
     * @return county info or null
     */
    public CountyInfo getCountyInfo()
    {
        return mCountyInfo;
    }

    /**
     * Sets the county info
     *
     * @param countyInfo or null
     */
    public void setCountyInfo(CountyInfo countyInfo)
    {
        mCountyInfo = countyInfo;
    }

    /**
     * Formatted system identity
     *
     * @return
     */
    public String getSystemFormatted()
    {
        if(mSite != null)
        {
            //System number is stored in the zone number field.
            return format(mSite.getZoneNumber());
        }

        return null;
    }

    public String getSiteFormatted()
    {
        if(mSite != null)
        {
            return format(mSite.getSiteNumber());
        }

        return null;
    }

    /**
     * Optional site RFSS value
     */
    public String getRfssFormatted()
    {
        if(mSite != null)
        {
            return format(mSite.getRfss());
        }

        return null;
    }

    /**
     * Optional county name
     */
    public String getCountyName()
    {
        if(mCountyInfo != null)
        {
            return mCountyInfo.getName();
        }

        return null;
    }

    /**
     * Optional description of the site
     */
    public String getDescription()
    {
        if(mSite != null)
        {
            return mSite.getDescription() + (mSite.getTdmaControlChannel() > 0 ? " (TDMA CC)" : "");
        }

        return null;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        String system = getSystemFormatted();
        sb.append(system == null ? "-" : system).append(" ");
        String rfss = getRfssFormatted();
        sb.append(rfss == null ? "-" : rfss).append(" ");
        String site = getSiteFormatted();
        sb.append(site == null ? "-" : site);
        return sb.toString();
    }

    @Override
    public int compareTo(EnrichedSite o)
    {
        return this.toString().compareTo(o.toString());
    }
}
