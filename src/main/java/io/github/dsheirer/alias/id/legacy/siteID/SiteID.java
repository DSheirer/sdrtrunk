/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.alias.id.legacy.siteID;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;

public class SiteID extends AliasID
{
    private String mSite;

    public SiteID()
    {
    }

    @Override
    public boolean isAudioIdentifier()
    {
        return false;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "site")
    public String getSite()
    {
        return mSite;
    }

    public void setSite(String site)
    {
        mSite = site;
    }

    @Override
    public boolean isValid()
    {
        return mSite != null;
    }

    public String toString()
    {
        return "Site: " + (mSite != null ? mSite : "(empty)") + (isValid() ? "" : " **NOT VALID**");
    }

    @Override
    public boolean matches(AliasID otherID)
    {
        return otherID != null &&
            otherID instanceof SiteID &&
            getSite().contentEquals(((SiteID)otherID).getSite());
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public AliasIDType getType()
    {
        return AliasIDType.SITE;
    }
}
