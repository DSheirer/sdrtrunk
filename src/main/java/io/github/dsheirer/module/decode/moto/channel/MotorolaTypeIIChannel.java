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

package io.github.dsheirer.module.decode.moto.channel;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.module.decode.moto.Bandplan;
import io.github.dsheirer.protocol.Protocol;

import java.util.Objects;

public class MotorolaTypeIIChannel implements IChannelDescriptor
{
    private final int mChannelNumber;
    private final Bandplan mBandplan;

    public MotorolaTypeIIChannel(int channelNumber, Bandplan bandplan)
    {
        mChannelNumber = channelNumber;
        mBandplan = bandplan;
    }

    public int getChannelNumber()
    {
        return mChannelNumber;
    }

    @Override
    public long getDownlinkFrequency()
    {
        if(mBandplan == null)
        {
            return 0;
        }

        double freqMHz = mBandplan.getDownlinkFrequency(mChannelNumber);
        return freqMHz > 0 ? (long)(freqMHz * 1e6) : 0;
    }

    @Override
    public long getUplinkFrequency()
    {
        if(mBandplan == null)
        {
            return 0;
        }

        double freqMHz = mBandplan.getUplinkFrequency(mChannelNumber);
        return freqMHz > 0 ? (long)(freqMHz * 1e6) : 0;
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
        return Protocol.MOTOROLA_TYPE_II;
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

        MotorolaTypeIIChannel that = (MotorolaTypeIIChannel)o;
        return mChannelNumber == that.mChannelNumber;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mChannelNumber);
    }

    @Override
    public String toString()
    {
        return String.valueOf(mChannelNumber);
    }
}
