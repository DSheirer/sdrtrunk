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
package controller.channel;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso( ChannelMap.class )
public class ChannelMapList
{
    private ArrayList<ChannelMap> mChannelMap = new ArrayList<ChannelMap>();

    public ChannelMapList()
    {
    	/* Empty constructor for JAXB */
    }
    
	@XmlElement( name = "channel_map" )
    public ArrayList<ChannelMap> getChannelMap()
    {
        return mChannelMap;
    }
    
    public void setChannelMap( ArrayList<ChannelMap> ChannelMaps )
    {
    	mChannelMap = ChannelMaps;
    }
    
    public void addChannelMap( ChannelMap ChannelMap )
    {
        mChannelMap.add( ChannelMap );
    }
    
    public void removeChannelMap( ChannelMap ChannelMap )
    {
        mChannelMap.remove( ChannelMap );
    }
    
    public void clearChannelMaps()
    {
        mChannelMap.clear();
    }
    
	/**
	 * Gets the named channel map, or returns an empty map
	 */
	public ChannelMap getChannelMap( String name )
	{
		for( ChannelMap map: mChannelMap )
		{
			if( map.getName().equalsIgnoreCase( name ) )
			{
				return map;
			}
		}
		
		return new ChannelMap();
	}
}
