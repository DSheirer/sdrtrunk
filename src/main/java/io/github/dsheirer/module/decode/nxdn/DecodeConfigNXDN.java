/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.module.decode.nxdn;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.nxdn.channel.ChannelFrequency;
import io.github.dsheirer.module.decode.nxdn.layer3.proprietary.Encoding;
import io.github.dsheirer.module.decode.nxdn.layer3.type.TransmissionMode;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import java.util.ArrayList;
import java.util.List;

/**
 * NXDN decoder configuration
 */
public class DecodeConfigNXDN extends DecodeConfiguration
{
    public static final int CHANNEL_ROTATION_DELAY_MINIMUM_MS = 200;
    public static final int CHANNEL_ROTATION_DELAY_DEFAULT_MS = 500;
    public static final int CHANNEL_ROTATION_DELAY_MAXIMUM_MS = 2000;
    private int mTrafficChannelPoolSize = TRAFFIC_CHANNEL_LIMIT_DEFAULT;
    private boolean mIgnoreDataCalls = false;

    private static final ChannelSpecification CHANNEL_4800 = new ChannelSpecification(9600.0, 6250, 3000.0, 3125.0);
    private static final ChannelSpecification CHANNEL_9600 = new ChannelSpecification(19200.0, 12500, 5200.0, 6250.0);
    private TransmissionMode mTransmissionMode = TransmissionMode.M9600;
    private List<ChannelFrequency> mChannelMap = new ArrayList<>();
    private Encoding mEncoding = Encoding.UTF8;

    /**
     * Default constructor for Jackson.
     */
    public DecodeConfigNXDN()
    {
    }

    /**
     * Constructs an instance
     * @param transmissionMode for the system
     */
    public DecodeConfigNXDN(TransmissionMode transmissionMode)
    {
        mTransmissionMode = transmissionMode;
    }

    /**
     * Optional user-provided channel to frequency mapping
     * @return map
     */
    @JacksonXmlProperty(localName = "channelMap")
    public List<ChannelFrequency> getChannelMap()
    {
        return mChannelMap;
    }

    /**
     * Sets the optional user-provided channel to frequency mapping
     * @param channelMap with entries to configure channels
     */
    public void setChannelMap(List<ChannelFrequency> channelMap)
    {
        mChannelMap = channelMap;
    }

    /**
     * Adds the channel frequency entry to the map
     * @param channelFrequency to add
     */
    public void add(ChannelFrequency channelFrequency)
    {
        mChannelMap.add(channelFrequency);
    }

    /**
     * Ignore data calls
     * @return true if ignore
     */
    @JacksonXmlProperty(isAttribute = true, localName = "ignore_data_calls")
    public boolean isIgnoreDataCalls()
    {
        return mIgnoreDataCalls;
    }

    /**
     * Set ignore data calls
     * @param ignore data calls
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
     * Transmission mode for this system.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "mode")
    public TransmissionMode getTransmissionMode()
    {
        return mTransmissionMode;
    }

    /**
     * Sets/changes the transmission mode
     * @param transmissionMode to use
     */
    public void setTransmissionMode(TransmissionMode transmissionMode)
    {
        mTransmissionMode = transmissionMode;
    }

    /**
     * Character set for encoding talker alias values
     */
    @JacksonXmlProperty(isAttribute = true, localName = "charset")
    public Encoding getEncoding()
    {
        return mEncoding;
    }

    /**
     * Sets the character set for talker alias encoding
     * @param encoding to use
     */
    public void setEncoding(Encoding encoding)
    {
        mEncoding = encoding;
    }

    @JsonIgnore
    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.NXDN;
    }

    @JsonIgnore
    @Override
    public ChannelSpecification getChannelSpecification()
    {
        return getTransmissionMode() == TransmissionMode.M4800 ? CHANNEL_4800 : CHANNEL_9600;
    }
}
