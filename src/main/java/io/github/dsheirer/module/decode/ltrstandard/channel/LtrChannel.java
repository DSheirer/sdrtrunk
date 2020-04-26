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

package io.github.dsheirer.module.decode.ltrstandard.channel;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.protocol.Protocol;

public class LtrChannel implements IChannelDescriptor, Comparable<LtrChannel>
{
    private long mDownlink;
    private int mChannel;

    public LtrChannel(int channel)
    {
        mChannel = channel;
    }

    public int getChannel()
    {
        return mChannel;
    }

    public void setDownlink(long downlink)
    {
        mDownlink = downlink;
    }

    @Override
    public long getDownlinkFrequency()
    {
        return mDownlink;
    }

    @Override
    public long getUplinkFrequency()
    {
        return mDownlink;
    }

    @Override
    public int[] getFrequencyBandIdentifiers()
    {
        return new int[0];
    }

    @Override
    public void setFrequencyBand(IFrequencyBand bandIdentifier)
    {
        //No-op
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
        return Protocol.LTR;
    }

    @Override
    public String toString()
    {
        return String.valueOf(getChannel());
    }

    public String description()
    {
        return "TRANSMIT: " + getDownlinkFrequency();
    }

    @Override
    public int compareTo(LtrChannel o)
    {
        return Integer.compare(getChannel(), o.getChannel());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LtrChannel)) return false;
        return compareTo((LtrChannel) o) == 0;
    }
}
