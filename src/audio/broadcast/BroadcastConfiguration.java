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
package audio.broadcast;

import audio.broadcast.icecast.IcecastTCPConfiguration;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

@XmlSeeAlso({IcecastTCPConfiguration.class})
@XmlRootElement(name = "stream")
public abstract class BroadcastConfiguration
{
    private BroadcastFormat mBroadcastFormat;
    private String mName;
    private String mHost;
    private int mPort;
    private String mPassword;

    public BroadcastConfiguration()
    {
        //No-arg constructor required for JAXB
    }

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
    @XmlAttribute(name = "type")
    public abstract BroadcastServerType getBroadcastServerType();

    /**
     * Name identifying this broadcast configuration.
     */
    @XmlAttribute(name = "name")
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
    @XmlAttribute(name = "host")
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
    @XmlAttribute(name = "port")
    public int getPort()
    {
        return mPort;
    }

    /**
     * Port number of the streaming server to where the broadcast will be directed.
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

    public SocketAddress getAddress()
    {
        return new InetSocketAddress(getHost(), getPort());
    }

    /**
     * Password to authenticate with the streaming server.
     */
    @XmlAttribute(name = "password")
    public String getPassword()
    {
        return mPassword;
    }

    /**
     * Sets the password used to authenticate the broadcast broadcast user to the streaming server
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
     * Audio broadcast content type (e.g. mp3)
     */
    @XmlElement(name = "format")
    public BroadcastFormat getBroadcastFormat()
    {
        return mBroadcastFormat;
    }
}
