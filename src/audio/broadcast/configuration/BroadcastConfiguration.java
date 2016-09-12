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
package audio.broadcast.configuration;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public abstract class BroadcastConfiguration
{
    private BroadcastFormat mBroadcastFormat;
    private String mAlias;
    private String mHost;
    private int mPort;
    private String mPassword;


    /**
     * Broadcast audio streaming configuration.  Describes the streaming server, broadcast user authentication and
     * audio broadcast details to use when connecting to a streaming server and broadcasting audio.
     */
    public BroadcastConfiguration(BroadcastFormat format)
    {
        mBroadcastFormat = format;
    }

    public abstract BroadcastServerType getBroadcastServerType();

    /**
     * Alias identifying this broadcast configuration.
     */
    public String getAlias()
    {
        return mAlias;
    }

    /**
     * Sets the alias name that identifies/describes this configuration
     * @param alias
     */
    public void setAlias(String alias)
    {
        mAlias = alias;
    }

    /**
     * Streaming server host name.
     */
    public String getHost()
    {
        return mHost;
    }

    /**
     * Host name or IP address of the streaming server
     * @param host
     */
    public void setHost(String host)
    {
        mHost = host;
    }

    /**
     * Streaming server port number;
     */
    public int getPort()
    {
        return mPort;
    }

    /**
     * Port number of the streaming server to where the broadcast will be directed.
     * @param port
     */
    public void setPort(int port)
    {
        mPort = port;
    }

    public SocketAddress getAddress()
    {
        return new InetSocketAddress(getHost(), getPort());
    }

    /**
     * Password to authenticate with the streaming server.
     */
    public String getPassword()
    {
        return mPassword;
    }

    /**
     * Sets the password used to authenticate the broadcast broadcast user to the streaming server
     * @param password
     */
    public void setPassword(String password)
    {
        mPassword = password;
    }

    /**
     * Audio broadcast content type (e.g. mp3)
     */
    public BroadcastFormat getBroadcastFormat()
    {
        return mBroadcastFormat;
    }

    /**
     * Audio broadcast content type (e.g. mp3)
     * @param broadcastFormat
     */
    public void setBroadcastFormat(BroadcastFormat broadcastFormat)
    {
        mBroadcastFormat = broadcastFormat;
    }

}
