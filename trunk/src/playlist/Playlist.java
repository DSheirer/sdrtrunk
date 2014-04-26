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
package playlist;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import alias.AliasDirectory;
import controller.channel.ChannelMapList;
import controller.system.SystemList;

@XmlSeeAlso( { AliasDirectory.class,
			   ChannelMapList.class,
			   SystemList.class } )

@XmlRootElement( name = "playlist" )
public class Playlist
{
	private AliasDirectory mAliasDirectory = new AliasDirectory();
	private ChannelMapList mChannelMapList = new ChannelMapList();
	private SystemList mSystemList = new SystemList();
	
	public Playlist()
	{
	}
	
	@XmlElement( name = "alias_directory" )
	public AliasDirectory getAliasDirectory()
	{
		mAliasDirectory.refresh();
		
		return mAliasDirectory;
	}
	
	public void setAliasDirectory( AliasDirectory directory )
	{
		mAliasDirectory = directory;
	}

	@XmlElement( name = "system_list" )
	public SystemList getSystemList()
	{
		return mSystemList;
	}

	public void setSystemList( SystemList list )
	{
		mSystemList = list;
	}
	
	@XmlElement( name = "channel_maps" )
	public ChannelMapList getChannelMapList()
	{
		return mChannelMapList;
	}
	
	public void setChannelMapList( ChannelMapList list )
	{
		mChannelMapList = list;
	}
}
