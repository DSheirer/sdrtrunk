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
package io.github.dsheirer.audio.broadcast;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.github.dsheirer.audio.broadcast.icecast.IcecastConfiguration;
import io.github.dsheirer.audio.broadcast.shoutcast.v1.ShoutcastV1Configuration;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ShoutcastV2Configuration;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = IcecastConfiguration.class, name="icecastConfiguration"),
    @JsonSubTypes.Type(value = ShoutcastV1Configuration.class, name="shoutcastV1Configuration"),
    @JsonSubTypes.Type(value = ShoutcastV2Configuration.class, name="shoutcastV2Configuration"),
})
@JacksonXmlRootElement(localName = "stream")
public abstract class BroadcastConfiguration
{
    private BroadcastFormat mBroadcastFormat = BroadcastFormat.MP3;
    private String mName;
    private String mHost;
    private int mPort;
    private String mPassword;
    private long mDelay;
    private long mMaximumRecordingAge = 10 * 60 * 1000; //10 minutes default
    private boolean mEnabled = true;

    public BroadcastConfiguration()
    {
        //No-arg constructor required for JAXB
    }

    /**
     * Creates a copy/clone of the configuration
     */
    public abstract BroadcastConfiguration copyOf();

    /**
     * Broadcast audio streaming configuration.  Describes the streaming server and configuration details.
     */
    public BroadcastConfiguration(BroadcastFormat format)
    {
        mBroadcastFormat = format;
    }

    /**
     * Broadcast server type
     */
    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public abstract BroadcastServerType getBroadcastServerType();

    private void setBroadcastServerType(BroadcastServerType type)
    {
        //Do nothing
    }

    /**
     * Name identifying this broadcastAudio configuration.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "name")
    public String getName()
    {
        return mName;
    }

    /**
     * Sets the name name that identifies/describes this configuration
     *
     * @param name
     */
    public void setName(String name)
    {
        mName = name;
    }

    /**
     * Indicates if this configuration has a name value
     */
    public boolean hasName()
    {
        return mName != null;
    }

    /**
     * BROADCAST server host name.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "host")
    public String getHost()
    {
        return mHost;
    }

    /**
     * Host name or IP address of the streaming server
     *
     * @param host
     */
    public void setHost(String host)
    {
        mHost = host;
    }

    /**
     * Indicates if this configuration has a host value
     */
    public boolean hasHost()
    {
        return mHost != null;
    }

    /**
     * BROADCAST server port number;
     */
    @JacksonXmlProperty(isAttribute = true, localName = "port")
    public int getPort()
    {
        return mPort;
    }

    /**
     * Port number of the streaming server to where the broadcastAudio will be directed.
     *
     * @param port
     */
    public void setPort(int port)
    {
        mPort = port;
    }

    /**
     * Indicates if this configuration contains a port value
     */
    public boolean hasPort()
    {
        return mPort > 0;
    }

    @JsonIgnore
    public SocketAddress getAddress()
    {
        return new InetSocketAddress(getHost(), getPort());
    }

    /**
     * Password to authenticate with the streaming server.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "password")
    public String getPassword()
    {
        return mPassword;
    }

    /**
     * Sets the password used to authenticate the broadcastAudio broadcastAudio user to the streaming server
     *
     * @param password
     */
    public void setPassword(String password)
    {
        mPassword = password;
    }

    /**
     * Indicates if this configuration has a password value
     */
    public boolean hasPassword()
    {
        return mPassword != null;
    }

    /**
     * Audio broadcastAudio content type (e.g. mp3)
     */
    @JacksonXmlProperty(isAttribute = false, localName = "format")
    public BroadcastFormat getBroadcastFormat()
    {
        return mBroadcastFormat;
    }

    public void setBroadcastFormat(BroadcastFormat format)
    {
        mBroadcastFormat = format;
    }

    /**
     * Audio broadcastAudio delay from recording start until the audio file is broadcastAudio to the server.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "delay")
    public long getDelay()
    {
        return mDelay;
    }

    /**
     * User specified delay in milli-seconds from audio call start until when the audio is broadcastAudio to the server.
     * @param delay in milliseconds
     */
    public void setDelay(long delay)
    {
        mDelay = delay;
    }

    /**
     * Gets maximum recording age which determines the maximum amount of elapsed time that a recording can set in the
     * queue awaiting streaming before it is purged from the queue.  This value is in addition to the delay setting.
     * @return age in milliseconds
     */
    @JacksonXmlProperty(isAttribute = true, localName = "maximum_recording_age")
    public long getMaximumRecordingAge()
    {
        return mMaximumRecordingAge;
    }

    /**
     * Sets maximum recording age which determines the maximum amount of elapsed time that a recording can set in the
     * queue awaiting streaming before it is purged from the queue.  This value is in addition to the delay setting.
     * @param age in milliseconds
     */
    public void setMaximumRecordingAge(long age)
    {
        mMaximumRecordingAge = age;
    }

    /**
     * Indicates if this broadcaster is enable, meaning that it will automatically connect on startup.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "enabled")
    public boolean isEnabled()
    {
        return mEnabled;
    }

    public void setEnabled(boolean enabled)
    {
        mEnabled = enabled;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof BroadcastConfiguration))
        {
            return false;
        }

        BroadcastConfiguration that = (BroadcastConfiguration) o;

        if (getBroadcastFormat() != that.getBroadcastFormat())
        {
            return false;
        }

        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null)
        {
            return false;
        }

        return getBroadcastServerType() == that.getBroadcastServerType();
    }

    @Override
    public int hashCode()
    {
        int result = getBroadcastFormat().hashCode();
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + getBroadcastServerType().hashCode();
        return result;
    }

    /**
     * Indicates if this configuration is valid.  A minimal check will ensure that it contains at least a hostname and
     * port number.
     */
    @JsonIgnore
    public boolean isValid()
    {
        return mHost != null && mPort > 0;
    }
}
