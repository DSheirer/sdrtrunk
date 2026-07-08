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

import io.github.dsheirer.module.decode.nxdn.layer3.type.Bandwidth;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ChannelAccessInformation;
import java.util.Map;

/**
 * NXDN channel that uses Direct Frequency Assignment.
 */
public class NXDNChannelDFA extends NXDNChannel
{
    private ChannelAccessInformation mChannelAccessInformation;
    private final int mOutboundChannelNumber;
    private final int mInboundChannelNumber;

    /**
     * Constructs an instance
     * @param outboundChannelNumber from message
     * @param inboundChannelNumber from message
     * @param bandwidth from message
     */
    public NXDNChannelDFA(int outboundChannelNumber, int inboundChannelNumber, Bandwidth bandwidth)
    {
        mOutboundChannelNumber = outboundChannelNumber;
        mInboundChannelNumber = inboundChannelNumber;
    }

    @Override
    public void receive(ChannelAccessInformation channelAccessInformation, Map<Integer, ChannelFrequency> channelFrequencyMap)
    {
        mChannelAccessInformation = channelAccessInformation;
    }

    @Override
    public long getDownlinkFrequency()
    {
        if(mChannelAccessInformation != null)
        {
            return mChannelAccessInformation.getFrequency(mOutboundChannelNumber);
        }

        return 0;
    }

    @Override
    public long getUplinkFrequency()
    {
        if(mChannelAccessInformation != null)
        {
            return mChannelAccessInformation.getFrequency(mInboundChannelNumber);
        }

        return 0;
    }
}
