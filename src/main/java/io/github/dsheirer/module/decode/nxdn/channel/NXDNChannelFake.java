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
 * Fake channel that uses a talkgroup value for tracking call detection events by frequency, where the talkgroup value
 * represents the tracked frequency.
 */
public class NXDNChannelFake extends NXDNChannel
{
    private int mTalkgroup;

    public NXDNChannelFake(int talkgroup)
    {
        mTalkgroup = talkgroup;
    }

    /**
     * Always return false to indicate this a a non-value, fake channel.
     */
    @Override
    public boolean isValid()
    {
        return false;
    }

    @Override
    public long getDownlinkFrequency()
    {
        return mTalkgroup;
    }

    @Override
    public long getUplinkFrequency()
    {
        return 0;
    }

    @Override
    public void receive(ChannelAccessInformation channelAccessInformation, Map<Integer, ChannelFrequency> channelFrequencyMap)
    {
        //No-op.
    }

    @Override
    public String toString()
    {
        return "UNKNOWN";
    }
}
