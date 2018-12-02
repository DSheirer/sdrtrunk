/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.controller.channel;

import io.github.dsheirer.channel.IChannelDescriptor;

/**
 * Channel grant event with a channel descriptor that identifies the channel that is being created
 */
public class ChannelGrantEvent extends ChannelEvent
{
    private IChannelDescriptor mChannelDescriptor;

    /**
     * Constructs a channel grant event
     * @param channel with setup/configuration details
     * @param event to convey for the channel
     * @param channelDescriptor for the channel that is to be created
     */
    public ChannelGrantEvent(Channel channel, Event event, IChannelDescriptor channelDescriptor)
    {
        super(channel, event);
        mChannelDescriptor = channelDescriptor;
    }

    /**
     * Channel descriptor that contains uplink and downlink information
     */
    public IChannelDescriptor getChannelDescriptor()
    {
        return mChannelDescriptor;
    }
}
