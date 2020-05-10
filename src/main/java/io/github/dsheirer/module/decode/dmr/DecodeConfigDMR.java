/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2020 Dennis Sheirer, Zhenyu Mao
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.dmr;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

/**
 * DMR Decoder Configuration
 */
public class DecodeConfigDMR extends DecodeConfiguration
{
    private int mTrafficChannelPoolSize = TRAFFIC_CHANNEL_LIMIT_DEFAULT;
    private boolean mIgnoreDataCalls = true;

    public DecodeConfigDMR()
    {
    }

    /**
     * Overrides the default value to indicate that DMR has two timeslots
     */
    @Override
    public int getTimeslotCount()
    {
        return 2;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public DecoderType getDecoderType()
    {
        return DecoderType.DMR;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "ignore_data_calls")
    public boolean getIgnoreDataCalls()
    {
        return mIgnoreDataCalls;
    }

    public void setIgnoreDataCalls(boolean ignore)
    {
        mIgnoreDataCalls = ignore;
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
        return new ChannelSpecification(50000.0, 12500, 5750.0, 6500.0);
    }
}
