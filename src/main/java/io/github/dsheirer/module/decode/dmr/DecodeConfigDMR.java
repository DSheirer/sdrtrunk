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
import io.github.dsheirer.module.decode.dmr.channel.TimeslotFrequency;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

import java.util.ArrayList;
import java.util.List;

/**
 * DMR Decoder Configuration
 */
public class DecodeConfigDMR extends DecodeConfiguration
{
    private int mTrafficChannelPoolSize = TRAFFIC_CHANNEL_LIMIT_DEFAULT;
    private boolean mIgnoreDataCalls = true;
    private List<TimeslotFrequency> mTimeslotMap = new ArrayList<>();

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

    /**
     * Timeslot identifiers
     */
    @Override
    public int[] getTimeslots()
    {
        int[] timeslots = new int[2];
        timeslots[0] = 1;
        timeslots[1] = 2;
        return timeslots;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public DecoderType getDecoderType()
    {
        return DecoderType.DMR;
    }

    /**
     * Indicates if traffic channel grants for data calls should be ignored
     */
    @JacksonXmlProperty(isAttribute = true, localName = "ignore_data_calls")
    public boolean getIgnoreDataCalls()
    {
        return mIgnoreDataCalls;
    }

    /**
     * Sets flag to ignore data call traffic channel grants
     * @param ignore true to ignore data calls.
     */
    public void setIgnoreDataCalls(boolean ignore)
    {
        mIgnoreDataCalls = ignore;
    }

    /**
     * Traffic channel pool size.
     * @return
     */
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
        return new ChannelSpecification(50000.0, 12500, 6500.0, 7200.0);
    }

    /**
     * Timeslot map for this decode configuration.
     *
     * Note: this is only used for MotoTRBO systems
     */
    @JacksonXmlProperty(isAttribute = false, localName = "timeslot")
    public List<TimeslotFrequency> getTimeslotMap()
    {
        return mTimeslotMap;
    }

    /**
     * Sets the timeslot map
     * @param timeslots for a MotoTRBO system
     */
    public void setTimeslotMap(List<TimeslotFrequency> timeslots)
    {
        mTimeslotMap = timeslots;
    }

    /**
     * Adds a timeslot frequency to the map
     */
    @JsonIgnore
    public void addTimeslotFrequency(TimeslotFrequency timeslotFrequency)
    {
        mTimeslotMap.add(timeslotFrequency);
    }
}
