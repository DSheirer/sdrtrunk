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
package ua.in.smartjava.alias.id.broadcast;

import ua.in.smartjava.alias.id.AliasID;
import ua.in.smartjava.alias.id.AliasIDType;

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

    @Override
    public boolean equals(Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof BroadcastChannel))
        {
            return false;
        }

        BroadcastChannel that = (BroadcastChannel)o;

        return getChannelName() != null ? getChannelName().equals(that.getChannelName()) : that.getChannelName() == null;
    }

    @Override
    public int hashCode()
    {
        return getChannelName() != null ? getChannelName().hashCode() : 0;
    }

    /**
     * Creates a named broadcast ua.in.smartjava.channel
     */
    public BroadcastChannel(String channelName)
    {
        mChannelName = channelName;
    }

    /**
     * Name of the broadcastAudio ua.in.smartjava.channel configuration
     */
    @XmlAttribute(name = "ua/in/smartjava/channel")
    public String getChannelName()
    {
        return mChannelName;
    }

    /**
     * Sets the name of the broadcastAudio ua.in.smartjava.channel configuration
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
    public boolean matches(AliasID id)
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
