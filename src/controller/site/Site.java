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
package controller.site;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import controller.channel.Channel;

@XmlSeeAlso( { Channel.class } )
@XmlRootElement( name = "site" )
public class Site
{
    private String mName;
    
    private ArrayList<Channel> mChannel = new ArrayList<Channel>();

    public Site()
    {
        this( "New Site" );
    }
    
    public Site( String name )
    {
        mName = name;
    }
    
    public String toString()
    {
        return mName;
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

    public ArrayList<Channel> getChannel()
    {
        return mChannel;
    }
    
    public void setChannel( ArrayList<Channel> configs )
    {
    	mChannel = configs;
    }
    
    public void addChannel( Channel channel )
    {
        mChannel.add( channel );
    }
    
    public void removeChannel( Channel channel )
    {
        mChannel.remove( channel );
    }
    
    public void removeAllChannels()
    {
        mChannel.clear();
    }
}
