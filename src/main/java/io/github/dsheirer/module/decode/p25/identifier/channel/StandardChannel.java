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

package io.github.dsheirer.module.decode.p25.identifier.channel;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.protocol.Protocol;
import java.text.DecimalFormat;

/**
 * Standard AM/FM channel with discrete frequency
 */
public class StandardChannel implements IChannelDescriptor
{
    private static final DecimalFormat FREQUENCY_FORMATTER = new DecimalFormat("0.0000");
    private long mFrequency;

    /**
     * Constructs an instance
     * @param frequency of the channel (Hz)
     */
    public StandardChannel(long frequency)
    {
        mFrequency = frequency;
    }

    @Override
    public long getDownlinkFrequency()
    {
        return mFrequency;
    }

    @Override
    public long getUplinkFrequency()
    {
        return mFrequency;
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
        return Protocol.UNKNOWN;
    }

    @Override
    public String toString()
    {
        return FREQUENCY_FORMATTER.format(mFrequency / 1E6d) + " MHz";
    }
}
