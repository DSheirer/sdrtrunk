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

package io.github.dsheirer.module.decode.smartnet;

import io.github.dsheirer.channel.IBandChannelDescriptor;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.protocol.Protocol;
import java.util.Objects;

/**
 * A Motorola Type II channel number and its downlink frequency from the band plan.
 */
public class SmartNetChannel implements IBandChannelDescriptor
{
    private final int mChannelNumber;
    private final long mFrequency;

    public SmartNetChannel(int channelNumber, long frequency)
    {
        mChannelNumber = channelNumber;
        mFrequency = frequency;
    }

    public static SmartNetChannel create(int channelNumber, SmartNetBandPlan bandPlan)
    {
        return new SmartNetChannel(channelNumber, bandPlan.getFrequency(channelNumber));
    }

    public int getChannelNumber()
    {
        return mChannelNumber;
    }

    @Override
    public long getDownlinkFrequency()
    {
        return mFrequency;
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
        //no-op: the band plan gives the frequency
    }

    @Override
    public boolean isTDMAChannel()
    {
        return false;
    }

    @Override
    public int getTimeslotCount()
    {
        return 1;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.SMARTNET;
    }

    @Override
    public String toString()
    {
        return mChannelNumber + " (" + String.format("%.4f", mFrequency / 1e6) + ")";
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof SmartNetChannel other && other.mChannelNumber == mChannelNumber &&
            other.mFrequency == mFrequency;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mChannelNumber, mFrequency);
    }
}
