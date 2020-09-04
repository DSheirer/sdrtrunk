/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.channel.state;

import io.github.dsheirer.controller.channel.Channel.ChannelType;

/**
 * Allows decoders to change the call timeout setting in the channel state
 */
public class ChangeChannelTimeoutEvent extends DecoderStateEvent
{
    private ChannelType mChannelType;
    private long mCallTimeoutMilliseconds;

    /**
     * Constructs an instance
     * @param source of the event
     * @param channelType for the event
     * @param timeout in milliseconds
     * @param timeslot of the event
     */
    public ChangeChannelTimeoutEvent(Object source, ChannelType channelType, long timeout, int timeslot)
    {
        super(source, Event.REQUEST_CHANGE_CALL_TIMEOUT, State.IDLE, timeslot);
        mChannelType = channelType;
        mCallTimeoutMilliseconds = timeout;
    }

    /**
     * Constructs an instance
     * @param source of the event
     * @param channelType for the event
     * @param timeout value in milliseconds
     */
    public ChangeChannelTimeoutEvent(Object source, ChannelType channelType, long timeout)
    {
        this(source, channelType, timeout, 0);
    }

    public ChannelType getChannelType()
    {
        return mChannelType;
    }

    /**
     * Call timeout value in milliseconds
     */
    public long getCallTimeoutMilliseconds()
    {
        return mCallTimeoutMilliseconds;
    }
}