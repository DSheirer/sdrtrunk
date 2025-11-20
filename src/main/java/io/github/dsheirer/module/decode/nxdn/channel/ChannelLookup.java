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
 * NXDN Channel Lookup for a channel table.
 */
public class ChannelLookup
{
    private final int mChannelNumber;
    private final long mDownlinkFrequency;
    private final long mUplinkFrequency;
    private final TransmissionMode mTransmissionMode;

    /**
     * Constructs an instance
     * @param number for the channel (1 - 2048)
     * @param down link frequency in Hertz
     * @param up link frequency in Hertz
     */
    public ChannelLookup(int number, long down, long up, TransmissionMode transmissionMode)
    {
        mChannelNumber = number;
        mDownlinkFrequency = down;
        mUplinkFrequency = up;
        mTransmissionMode = transmissionMode;
    }

    /**
     * Transmission mode
     * @return mode 4800 or 9600
     */
    public TransmissionMode getTransmissionMode()
    {
        return mTransmissionMode;
    }

    /**
     * Channel number
     * @return channel number 1-2048
     */
    public int getChannelNumber()
    {
        return mChannelNumber;
    }

    /**
     * Downlink frequency
     * @return frequency in Hertz
     */
    public long getDownlinkFrequency()
    {
        return mDownlinkFrequency;
    }

    /**
     * Uplink frequency
     * @return frequency in Hertz
     */
    public long getUplinkFrequency()
    {
        return mUplinkFrequency;
    }
}
