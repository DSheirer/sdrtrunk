/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
 *
 ******************************************************************************/
package alias.id.broadcast;

import alias.id.AliasID;
import alias.id.AliasIDType;

import javax.xml.bind.annotation.XmlAttribute;

public class BroadcastChannel extends AliasID implements Comparable<BroadcastChannel>
{
	private String mChannelName;

	public BroadcastChannel()
	{
		//JAXB Constructor
	}

	@Override
	public int compareTo(BroadcastChannel other)
	{
		if(mChannelName != null && other.getChannelName() != null)
		{
			return mChannelName.compareTo(other.getChannelName());
		}
		else if(mChannelName != null)
		{
			return -1;
		}
		else
		{
			return 1;
		}
	}

	/**
	 * Creates a named broadcast channel
	 */
	public BroadcastChannel(String channelName)
	{
		mChannelName = channelName;
	}

	/**
	 * Name of the broadcastAudio channel configuration
	 */
	@XmlAttribute(name="channel")
	public String getChannelName()
	{
		return mChannelName;
	}

	/**
	 * Sets the name of the broadcastAudio channel configuration
	 */
	public void setChannelName(String channel)
	{
		mChannelName = channel;
	}

	@Override
	public AliasIDType getType()
	{
		return AliasIDType.BROADCAST_CHANNEL;
	}

	@Override
	public boolean isValid()
	{
		return mChannelName != null;
	}

	@Override
	public boolean matches( AliasID id )
	{
		return false;
	}
	
	@Override
	public String toString()
	{
        if(isValid())
        {
            return "Broadcast Channel: " + mChannelName;
        }
        else
        {
            return "Broadcast Channel: None Selected";
        }
	}
}
