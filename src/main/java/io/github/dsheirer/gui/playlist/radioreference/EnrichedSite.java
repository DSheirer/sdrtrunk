/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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
public class EnrichedSite
{
    private Site mSite;
    private CountyInfo mCountyInfo;

    /**
     * Constructs an instance
     * @param site object
     * @param countyInfo optional
     */
    public EnrichedSite(Site site, CountyInfo countyInfo)
    {
        mSite = site;
        mCountyInfo = countyInfo;
    }

    /**
     * Site instance
     * @return site or null
     */
    public Site getSite()
    {
        return mSite;
    }

    /**
     * Sets the site instance
     * @param site or null
     */
    public void setSite(Site site)
    {
        mSite = site;
    }

    /**
     * County information
     * @return county info or null
     */
    public CountyInfo getCountyInfo()
    {
        return mCountyInfo;
    }

    /**
     * Sets the county info
     * @param countyInfo or null
     */
    public void setCountyInfo(CountyInfo countyInfo)
    {
        mCountyInfo = countyInfo;
    }

    /**
     * Optional site number
     */
    public Integer getSiteNumber()
    {
        if(mSite != null)
        {
            return mSite.getSiteNumber();
        }

        return null;
    }

    /**
     * Optional site RFSS value
     */
    public Integer getRfss()
    {
        if(mSite != null)
        {
            return mSite.getRfss();
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
            return mSite.getDescription();
        }

        return null;
    }
}
