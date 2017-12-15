/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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

import alias.Alias;
import audio.broadcast.BroadcastConfiguration;
import controller.channel.Channel;
import controller.channel.map.ChannelMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.ArrayList;
import java.util.List;

@XmlSeeAlso( { Alias.class,Channel.class,ChannelMap.class,BroadcastConfiguration.class} )
@XmlRootElement( name = "playlist" )
public class PlaylistV2
{
	private List<Alias> mAliases = new ArrayList<>();
	private List<BroadcastConfiguration> mBroadcastConfigurations = new ArrayList<>();
	private List<Channel> mChannels = new ArrayList<>();
	private List<ChannelMap> mChannelMaps = new ArrayList<>();
	
	public PlaylistV2()
	{
	}
	
	@XmlElement( name = "alias" )
	public List<Alias> getAliases()
	{
		return mAliases;
	}
	
	public void setAliases( List<Alias> aliases )
	{
		mAliases = aliases;
	}
	
	@XmlElement( name = "channel" )
	public List<Channel> getChannels()
	{
		return mChannels;
	}
	
	public void setChannels( List<Channel> channels )
	{
		mChannels = channels;
	}
	
	@XmlElement( name = "channel_map" )
	public List<ChannelMap> getChannelMaps()
	{
		return mChannelMaps;
	}

	public void setChannelMaps( List<ChannelMap> channelMaps )
	{
		mChannelMaps = channelMaps;
	}

	@XmlElement(name = "stream")
	public List<BroadcastConfiguration> getBroadcastConfigurations()
	{
		return mBroadcastConfigurations;
	}

	public void setBroadcastConfigurations(List<BroadcastConfiguration> configurations)
	{
		mBroadcastConfigurations = configurations;
	}
}
