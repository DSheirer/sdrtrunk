/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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
package io.github.dsheirer.alias.id.broadcast;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;

public class BroadcastChannel extends AliasID implements Comparable<BroadcastChannel>
{
    private String mChannelName;

    public BroadcastChannel()
    {
        //JAXB Constructor
    }

    @Override
    public boolean isAudioIdentifier()
    {
        return true;
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
     * Creates a named broadcast channel
     */
    public BroadcastChannel(String channelName)
    {
        mChannelName = channelName;
    }

    /**
     * Name of the broadcastAudio channel configuration
     */
    @JacksonXmlProperty(isAttribute = true, localName = "channel")
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
        updateValueProperty();
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
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
        return isValid() ? mChannelName : "(invalid)";
    }
}
