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

package io.github.dsheirer.module.decode.nxdn.channel;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * NXDN Channel to Frequency mapping.
 */
public class ChannelFrequency
{
    private int mChannel;
    private long mDownlink;
    private long mUplink;

    /**
     * Constructs an instance
     * @param number for the channel (1 - 2048)
     * @param down link frequency in Hertz
     * @param up link frequency in Hertz
     */
    public ChannelFrequency(int number, long down, long up)
    {
        mChannel = number;
        mDownlink = down;
        mUplink = up;
    }

    /**
     * Empty constructor for Jackson deserialization
     */
    public ChannelFrequency()
    {
        //Empty Jackson constructor
    }

    /**
     * Channel number
     * @return channel number 1-2048
     */
    @JacksonXmlProperty(isAttribute = true, localName = "channel")
    public int getChannel()
    {
        return mChannel;
    }

    /**
     * Sets the channel number
     * @param channel in range 1 to 2048
     */
    public void setChannel(int channel)
    {
        mChannel = channel;
    }

    /**
     * Downlink frequency
     * @return frequency in Hertz
     */
    @JacksonXmlProperty(isAttribute = true, localName = "downlink")
    public long getDownlink()
    {
        return mDownlink;
    }

    /**
     * Sets the downlink frequency
     * @param downlink frequency in Hertz
     */
    public void setDownlink(long downlink)
    {
        mDownlink = downlink;
    }

    /**
     * Uplink frequency
     * @return frequency in Hertz
     */
    @JacksonXmlProperty(isAttribute = true, localName = "uplink")
    public long getUplink()
    {
        return mUplink;
    }

    /**
     * Sets the uplink frequency
     * @param uplink frequency in Hertz
     */
    public void setUplink(long uplink)
    {
        mUplink = uplink;
    }
}
