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
package io.github.dsheirer.module.decode.mpt1327;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

public class DecodeConfigMPT1327 extends DecodeConfiguration
{
    private String mChannelMapName;
    private Sync mSync = Sync.NORMAL;

    private int mCallTimeout = DEFAULT_CALL_TIMEOUT_SECONDS;
    private int mTrafficChannelPoolSize = TRAFFIC_CHANNEL_LIMIT_DEFAULT;

    public DecodeConfigMPT1327()
    {
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public DecoderType getDecoderType()
    {
        return DecoderType.MPT1327;
    }

    @JacksonXmlProperty(isAttribute = false, localName = "channelMapName")
    public String getChannelMapName()
    {
        return mChannelMapName;
    }

    public void setChannelMapName(String name)
    {
        mChannelMapName = name;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "sync")
    public Sync getSync()
    {
        return mSync;
    }

    public void setSync(Sync sync)
    {
        mSync = sync;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "call_timeout")
    public int getCallTimeout()
    {
        return mCallTimeout;
    }

    /**
     * Sets the call timeout value in seconds ( 10 - 600 );
     *
     * @param timeout
     */
    public void setCallTimeout(int timeout)
    {
        if(CALL_TIMEOUT_MINIMUM <= timeout && timeout <= CALL_TIMEOUT_MAXIMUM)
        {
            mCallTimeout = timeout;
        }
        else
        {
            mCallTimeout = DEFAULT_CALL_TIMEOUT_SECONDS;
        }
    }

    @JacksonXmlProperty(isAttribute = true, localName = "traffic_channel_pool_size")
    public int getTrafficChannelPoolSize()
    {
        return mTrafficChannelPoolSize;
    }

    /**
     * Sets the traffic channel pool size which is the maximum number of
     * simultaneous traffic channels that can be allocated.
     *
     * This limits the maximum calls so that busy systems won't cause more
     * traffic channels to be allocated than the decoder/software/host computer
     * can support.
     */
    public void setTrafficChannelPoolSize(int size)
    {
        mTrafficChannelPoolSize = size;
    }

    /**
     * Source channel specification for this decoder
     */
    @JsonIgnore
    @Override
    public ChannelSpecification getChannelSpecification()
    {
        return new ChannelSpecification(25000.0,
            12500, 6000.0, 7000.0);
    }
}
