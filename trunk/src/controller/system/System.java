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
package controller.system;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import controller.site.Site;

@XmlSeeAlso( { Site.class } )
@XmlRootElement( name = "system" )
public class System
{
    private String mName;
    private ArrayList<Site> mSite = new ArrayList<Site>();

    public System()
    {
        this( "New System" );
    }
    
    public System( String name )
    {
        mName = name;
    }
    
	@XmlAttribute
    public String getName()
    {
        return mName;
    }
    
    public void setName( String name )
    {
        mName = name;
    }
    
    public ArrayList<Site> getSite()
    {
        return mSite;
    }
    
    public void setSite( ArrayList<Site> sites )
    {
    	mSite = sites;
    }
    
    public void addSite( Site site )
    {
        mSite.add( site );
    }
    
    public void removeSite( Site site )
    {
        mSite.remove( site );
    }
    
    public void clearSites()
    {
        mSite.clear();
    }
    
    public String toString()
    {
        return mName;
    }
}
