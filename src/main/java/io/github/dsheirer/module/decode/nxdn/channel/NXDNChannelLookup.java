/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

import io.github.dsheirer.module.decode.nxdn.layer3.type.TransmissionMode;

/**
 * NXDN channel that uses a lookup table for the frequency values.
 */
public class NXDNChannelLookup extends NXDNChannel
{
    private ChannelLookup mChannelLookup;
    private final int mChannelNumber;

    /**
     * Constructs an instance
     * @param channelNumber for this channel referenced against a global system channel lookup table.
     */
    public NXDNChannelLookup(int channelNumber)
    {
        super(TransmissionMode.M9600); //As a default until the channel lookup is assigned.
        mChannelNumber = channelNumber;
    }

    @Override
    public TransmissionMode getTransmissionMode()
    {
        if(mChannelLookup != null)
        {
            return mChannelLookup.getTransmissionMode();
        }

        return super.getTransmissionMode();
    }

    /**
     * Sets the channel lookup from a lookup table.
     * @param channelLookup to assign
     */
    public void setChannelLookup(ChannelLookup channelLookup)
    {
        mChannelLookup = channelLookup;
    }

    /**
     * Channel number
     */
    public int getChannelNumber()
    {
        return mChannelNumber;
    }

    @Override
    public long getDownlinkFrequency()
    {
        if(mChannelLookup != null)
        {
            return mChannelLookup.getDownlinkFrequency();
        }

        return 0;
    }

    @Override
    public long getUplinkFrequency()
    {
        if(mChannelLookup != null)
        {
            return mChannelLookup.getUplinkFrequency();
        }

        return 0;
    }
}
