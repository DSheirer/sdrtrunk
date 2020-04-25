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

package io.github.dsheirer.module.decode.ltrnet.channel;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.protocol.Protocol;

public class LtrNetChannel implements IChannelDescriptor, Comparable<LtrNetChannel>
{
    private long mDownlink;
    private long mUplink;
    private int mChannel;

    public LtrNetChannel(int channel)
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

    public void setUplink(long uplink)
    {
        mUplink = uplink;
    }

    @Override
    public long getDownlinkFrequency()
    {
        return mDownlink;
    }

    @Override
    public long getUplinkFrequency()
    {
        return mUplink;
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
        return Protocol.LTR_NET;
    }

    @Override
    public String toString()
    {
        return String.valueOf(getChannel());
    }

    public String description()
    {
        return "TRANSMIT: " + getDownlinkFrequency() + " RECEIVE:" + getUplinkFrequency();
    }

    @Override
    public int compareTo(LtrNetChannel o)
    {
        return Integer.compare(getChannel(), o.getChannel());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LtrNetChannel)) return false;
        return compareTo((LtrNetChannel) o) == 0;
    }
}
