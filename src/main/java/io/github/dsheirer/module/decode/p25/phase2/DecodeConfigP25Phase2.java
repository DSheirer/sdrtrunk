/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
 * ****************************************************************************
 */
package io.github.dsheirer.module.decode.p25.phase2;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.module.decode.p25.phase2.message.P25P2Message;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

public class DecodeConfigP25Phase2 extends DecodeConfiguration
{
    private ScrambleParameters mScrambleParameters;
    private boolean mAutoDetectScrambleParameters;
    private boolean mIgnoreDataCalls = false;
    private int mTrafficChannelPoolSize = TRAFFIC_CHANNEL_LIMIT_DEFAULT;

    public DecodeConfigP25Phase2()
    {
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public DecoderType getDecoderType()
    {
        return DecoderType.P25_PHASE2;
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
     * Number of timeslots for this protocol.
     */
    @Override
    public int getTimeslotCount()
    {
        return 2;
    }

    @Override
    public int[] getTimeslots()
    {
        return new int[]{P25P2Message.TIMESLOT_1, P25P2Message.TIMESLOT_2};
    }

    /**
     * Optional user-provided scramble (ie randomizer) parameters to use for the channel.
     */
    @JacksonXmlProperty(localName = "scramble_parameters")
    public ScrambleParameters getScrambleParameters()
    {
        return mScrambleParameters;
    }

    /**
     * Sets the user-provided scramble parameters
     */
    public void setScrambleParameters(ScrambleParameters scrambleParameters)
    {
        mScrambleParameters = scrambleParameters;
    }

    /**
     * Indicates if the decoder will attempt to auto-detect scramble parameters.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "auto_detect_scramble_parameters")
    public boolean isAutoDetectScrambleParameters()
    {
        return mAutoDetectScrambleParameters;
    }

    /**
     * Sets the decoder to attempt to auto-detect scramble parameters.
     */
    public void setAutoDetectScrambleParameters(boolean autoDetect)
    {
        mAutoDetectScrambleParameters = autoDetect;
    }

    /**
     * Indicates if the decoder should ignore data calls.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "ignore_data_calls")
    public boolean getIgnoreDataCalls()
    {
        return mIgnoreDataCalls;
    }

    /**
     * Sets if the decoder should ignore data calls.
     */
    public void setIgnoreDataCalls(boolean ignore)
    {
        mIgnoreDataCalls = ignore;
    }

    /**
     * Traffic channel pool size which is the maximum number of simultaneous traffic channels that can be
     * allocated by the traffic channel manager.
     *
     * This limits the maximum calls so that busy systems won't cause more traffic channels to be allocated than the
     * decoder/software/host computer can support.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "traffic_channel_pool_size")
    public int getTrafficChannelPoolSize()
    {
        return mTrafficChannelPoolSize;
    }

    /**
     * Sets the traffic channel pool size.
     */
    public void setTrafficChannelPoolSize(int size)
    {
        mTrafficChannelPoolSize = size;
    }
}
