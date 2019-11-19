/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.module.decode.mpt1327.channel;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.controller.channel.map.ChannelMap;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.protocol.Protocol;

import java.util.Objects;

/**
 * MPT-1327 Channel
 */
public class MPT1327Channel implements IChannelDescriptor
{
    private int mChannelNumber;
    private ChannelMap mChannelMap;

    /**
     * Constructs a channel instance
     */
    public MPT1327Channel(int channelNumber)
    {
        mChannelNumber = channelNumber;
    }

    public String toString()
    {
        return String.valueOf(mChannelNumber);
    }

    /**
     * Channel number for this channel.
     */
    public int getChannelNumber()
    {
        return mChannelNumber;
    }

    /**
     * Assigns a channel map to this channel so that the frequency can be calculated.
     */
    public void setChannelMap(ChannelMap channelMap)
    {
        mChannelMap = channelMap;
    }

    @Override
    public long getDownlinkFrequency()
    {
        if(mChannelMap != null)
        {
            return mChannelMap.getFrequency(mChannelNumber);
        }

        return 0;
    }

    @Override
    public long getUplinkFrequency()
    {
        return 0;
    }

    @Override
    public int[] getFrequencyBandIdentifiers()
    {
        return new int[0];
    }

    @Override
    public void setFrequencyBand(IFrequencyBand bandIdentifier)
    {
        //no-op
    }

    @Override
    public boolean isTDMAChannel()
    {
        return false;
    }

    @Override
    public int getTimeslotCount()
    {
        return 0;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.MPT1327;
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(o == null || getClass() != o.getClass())
        {
            return false;
        }
        MPT1327Channel that = (MPT1327Channel)o;
        return getChannelNumber() == that.getChannelNumber();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getChannelNumber());
    }

    /**
     * Creates a new MPT1327 channel
     */
    public static MPT1327Channel create(int channelNumber)
    {
        return new MPT1327Channel(channelNumber);
    }
}
