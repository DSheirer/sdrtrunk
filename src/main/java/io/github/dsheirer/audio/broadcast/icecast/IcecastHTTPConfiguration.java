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
package io.github.dsheirer.audio.broadcast.icecast;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import javafx.beans.binding.Bindings;

@JsonSubTypes.Type(value=IcecastHTTPConfiguration.class, name="icecastHTTPConfiguration")
public class IcecastHTTPConfiguration extends IcecastConfiguration
{
    public IcecastHTTPConfiguration()
    {
        //no-arg JAXB constructor
        this(BroadcastFormat.MP3);
    }

    /**
     * Icecast 2.4.x+ compatible configuration
     */
    public IcecastHTTPConfiguration(BroadcastFormat format)
    {
        super(format);
        setPassword("change me!");

        mValid.bind(Bindings.and(Bindings.and(Bindings.and(Bindings.isNotNull(mHost), Bindings.greaterThan(mPort, 0)),
            Bindings.isNotNull(mMountPoint)), Bindings.isNotNull(mPassword)));
    }

    @Override
    public BroadcastConfiguration copyOf()
    {
        IcecastHTTPConfiguration copy = new IcecastHTTPConfiguration(getBroadcastFormat());

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
        copy.setBitRate(getBitRate());
        copy.setChannels(getChannels());
        copy.setSampleRate(getSampleRate());
        copy.setURL(getURL());

        return copy;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.ICECAST_HTTP;
    }
}
