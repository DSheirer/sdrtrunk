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
import java.util.List;

/**
 * Channel access and channel map information
 */
public class ChannelInformation
{
    private final ChannelAccessInformation mChannelAccessInformation;
    private final List<ChannelFrequency> mChannelFrequencies;

    /**
     * Constructs an instance
     * @param access info
     * @param map of optional channels/frequencies
     */
    public ChannelInformation(ChannelAccessInformation access, List<ChannelFrequency> channels)
    {
        mChannelAccessInformation = access;
        mChannelFrequencies = channels;
    }

    /**
     * Channel access info from the control channel
     * @return access info
     */
    public ChannelAccessInformation getChannelAccessInformation()
    {
        return mChannelAccessInformation;
    }

    /**
     * Channel frequencies
     * @return list
     */
    public List<ChannelFrequency> getChannelFrequencies()
    {
        return mChannelFrequencies;
    }
}
