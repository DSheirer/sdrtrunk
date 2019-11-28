/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.gui.playlist.radioreference;

import io.github.dsheirer.rrapi.type.SiteFrequency;

import java.util.EnumSet;
import java.util.List;

public enum SiteChannel
{
    CONTROL("Control"),
    CONTROL_AND_ALTERNATES("Control & Alternate(s)"),
    SELECTED("Selected"),
    ALL("All");

    private String mLabel;

    SiteChannel(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Provides an enumset applicable for the set of site frequencies.
     */
    public static EnumSet<SiteChannel> fromSiteFrequencies(List<SiteFrequency> frequencies)
    {
        boolean control = false;
        boolean alternate = true;

        for(SiteFrequency siteFrequency: frequencies)
        {
            String use = siteFrequency.getUse();

            if(use != null)
            {
                if(use.contentEquals("a"))
                {
                    alternate = true;
                }
                else if(use.contentEquals("d"))
                {
                    control = true;
                }
            }
        }

        if(!frequencies.isEmpty())
        {
            if(control && alternate)
            {
                return EnumSet.of(CONTROL, CONTROL_AND_ALTERNATES, ALL, SELECTED);
            }
            else if(control)
            {
                return EnumSet.of(CONTROL, ALL, SELECTED);
            }
            else
            {
                return EnumSet.of(ALL, SELECTED);
            }
        }

        return EnumSet.noneOf(SiteChannel.class);
    }
}
