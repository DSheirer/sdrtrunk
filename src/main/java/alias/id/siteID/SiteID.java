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
package alias.id.siteID;

import javax.xml.bind.annotation.XmlAttribute;

import alias.id.AliasID;
import alias.id.AliasIDType;

public class SiteID extends AliasID
{
	private String mSite;
	
	public SiteID()
	{
	}

	@XmlAttribute
	public String getSite()
	{
		return mSite;
	}

	public void setSite( String site )
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
		return "Site: " + mSite;
	}

	@Override
    public boolean matches( AliasID otherID )
    {
		return otherID != null && 
			   otherID instanceof SiteID && 
			   getSite().contentEquals( ( (SiteID)otherID).getSite() );
    }

	@Override
    public AliasIDType getType()
    {
	    return AliasIDType.SITE;
    }
}
