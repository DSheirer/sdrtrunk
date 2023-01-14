/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.controller.channel;

import io.github.dsheirer.module.ModuleEventBusMessage;

/**
 * Request to convert a currently processing standard channel type to a traffic channel type.  This request will
 * normally be handled by the ChannelProcessingManager instance.
 */
public class ChannelConversionRequest extends ModuleEventBusMessage
{
    private Channel mCurrentChannel;
    private Channel mTrafficChannel;

    /**
     * Constructs an instance.
     * @param currentChannel that is actively processing
     * @param trafficChannel to convert to
     */
    public ChannelConversionRequest(Channel currentChannel, Channel trafficChannel)
    {
        mCurrentChannel = currentChannel;
        mTrafficChannel = trafficChannel;
    }

    /**
     * Channel configuration for the channel that is currently processing
     */
    public Channel getCurrentChannel()
    {
        return mCurrentChannel;
    }

    /**
     * Channel configuration for the traffic channel to convert to
     */
    public Channel getTrafficChannel()
    {
        return mTrafficChannel;
    }
}
