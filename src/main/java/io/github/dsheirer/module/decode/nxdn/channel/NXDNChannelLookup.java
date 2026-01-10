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

import io.github.dsheirer.module.decode.nxdn.layer3.type.ChannelAccessInformation;
import java.util.Map;

/**
 * NXDN channel that uses a lookup table for the frequency values.
 */
public class NXDNChannelLookup extends NXDNChannel
{
    private ChannelFrequency mChannelFrequency;
    private final int mChannelNumber;

    /**
     * Constructs an instance
     * @param channelNumber for this channel referenced against a global system channel lookup table.
     */
    public NXDNChannelLookup(int channelNumber)
    {
        mChannelNumber = channelNumber;
    }

    @Override
    public void receive(ChannelAccessInformation channelAccessInformation, Map<Integer, ChannelFrequency> channelFrequencyMap)
    {
        mChannelFrequency = channelFrequencyMap.get(mChannelNumber);
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
        if(mChannelFrequency != null)
        {
            return mChannelFrequency.getDownlink();
        }

        return 0;
    }

    @Override
    public long getUplinkFrequency()
    {
        if(mChannelFrequency != null)
        {
            return mChannelFrequency.getUplink();
        }

        return 0;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(mChannelFrequency != null)
        {
            sb.append("DN:").append(getDownlinkFrequency() / 1E6D);

            long up = getUplinkFrequency();

            if(up > 0)
            {
                sb.append(" UP:").append(up / 1E6D);
            }

            sb.append(" MHZ");
        }
        else
        {
            sb.append("CHANNEL-MODE [" + mChannelNumber + "] IS MISSING CHANNEL:FREQUENCY MAPPING");
        }

        return sb.toString();
    }
}
