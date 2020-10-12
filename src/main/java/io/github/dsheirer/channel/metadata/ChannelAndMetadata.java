/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.channel.metadata;

import io.github.dsheirer.controller.channel.Channel;

import java.util.List;

/**
 * Channel and associated channel metadata
 */
public class ChannelAndMetadata
{
    private Channel mChannel;
    private List<ChannelMetadata> mChannelMetadata;

    /**
     * Connstructs an instance
     * @param channel
     * @param metadata
     */
    public ChannelAndMetadata(Channel channel, List<ChannelMetadata> metadata)
    {
        mChannel = channel;
        mChannelMetadata = metadata;
    }

    /**
     * Channel
     */
    public Channel getChannel()
    {
        return mChannel;
    }

    /**
     * Channel Metadata
     */
    public List<ChannelMetadata> getChannelMetadata()
    {
        return mChannelMetadata;
    }
}
