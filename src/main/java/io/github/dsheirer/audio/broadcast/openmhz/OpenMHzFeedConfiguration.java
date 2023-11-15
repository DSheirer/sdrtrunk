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
package io.github.dsheirer.audio.broadcast.openmhz;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.audio.broadcast.icecast.IcecastTCPConfiguration;
import io.github.dsheirer.rrapi.type.UserFeedBroadcast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenMHzFeedConfiguration extends IcecastTCPConfiguration
{
    private final static Logger mLog = LoggerFactory.getLogger(OpenMHzFeedConfiguration.class);

    private int mFeedID;

    public OpenMHzFeedConfiguration()
    {
        //No-arg constructor for JAXB
        this(BroadcastFormat.MP3);
    }

    /**
     * OpenMHz configuration for Icecast 2.3.2 compatible servers
     *
     * @param format of output audio (MP3)
     */
    public OpenMHzFeedConfiguration(BroadcastFormat format)
    {
        super(format);

        setBitRate(16);
        setChannels(1);
        setSampleRate(8000);
        setInline(true);
    }

    public static OpenMHzFeedConfiguration from(UserFeedBroadcast userFeedBroadcast)
    {
        OpenMHzFeedConfiguration config = new OpenMHzFeedConfiguration(BroadcastFormat.MP3);
        config.setName(userFeedBroadcast.getDescription());
        config.setHost(userFeedBroadcast.getHostname());
        config.setMountPoint(userFeedBroadcast.getMount());
        config.setFeedID(userFeedBroadcast.getFeedId());
        config.setPassword(userFeedBroadcast.getPassword());

        try
        {
            config.setPort(Integer.parseInt(userFeedBroadcast.getPort()));
        }
        catch(Exception e)
        {
            mLog.error("Error creating Rdio Scanner configuration from radio reference user feed instance");
        }

        return config;
    }

    @Override
    public BroadcastConfiguration copyOf()
    {
        OpenMHzFeedConfiguration copy = new OpenMHzFeedConfiguration(getBroadcastFormat());

        //Broadcast Configuration Parameters
        copy.setName(getName());
        copy.setHost(getHost());
        copy.setPort(getPort());
        copy.setInline(getInline());
        copy.setPassword(getPassword());
        copy.setDelay(getDelay());
        copy.setEnabled(false);

        //Icecast Configuration Parameters
        copy.setUserName(getUserName());
        copy.setMountPoint(getMountPoint());
        copy.setDescription(getDescription());
        copy.setGenre(getGenre());
        copy.setPublic(isPublic());
        copy.setURL(getURL());

        //OpenMHz Configuration Parameters
        copy.setFeedID(getFeedID());

        return copy;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.OPENMHZ;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "feed_id")
    public int getFeedID()
    {
        return mFeedID;
    }

    public void setFeedID(int feedID)
    {
        mFeedID = feedID;
    }
}