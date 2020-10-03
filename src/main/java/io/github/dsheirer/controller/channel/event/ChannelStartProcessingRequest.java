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

package io.github.dsheirer.controller.channel.event;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.module.decode.traffic.TrafficChannelManager;

/**
 * Request to start/enable processing for the specified channel
 */
public class ChannelStartProcessingRequest
{
    private Channel mChannel;
    private IChannelDescriptor mChannelDescriptor;
    private IdentifierCollection mIdentifierCollection;
    private TrafficChannelManager mTrafficChannelManager;
    private boolean mPersistentAttempt;

    /**
     * Constructs an instance
     * @param channel to start
     * @param channelDescriptor that identifies which channel in a multi-channel config to start
     * @param identifierCollection to use for the new channel
     * @param trafficChannelManager (optional) to use for the new channel
     */
    public ChannelStartProcessingRequest(Channel channel, IChannelDescriptor channelDescriptor,
                                         IdentifierCollection identifierCollection,
                                         TrafficChannelManager trafficChannelManager)
    {
        mChannel = channel;
        mChannelDescriptor = channelDescriptor;
        mIdentifierCollection = identifierCollection;
        mTrafficChannelManager = trafficChannelManager;
    }

    /**
     * Constructs an instance
     * @param channel to start
     * @param channelDescriptor that identifies which channel in a multi-channel config to start
     * @param identifierCollection to use for the new channel
     */
    public ChannelStartProcessingRequest(Channel channel, IChannelDescriptor channelDescriptor,
                                         IdentifierCollection identifierCollection)
    {
        this(channel, channelDescriptor, identifierCollection, null);
    }

    /**
     * Constructs an instance
     * @param channel to start processing
     * @param trafficChannelManager optional to use in the processing chain
     * @param channelDescriptor identifying which frequency to use in a multi-frequency configuration
     */
    public ChannelStartProcessingRequest(Channel channel, TrafficChannelManager trafficChannelManager)
    {
        this(channel, null, null, trafficChannelManager);
    }

    /**
     * Constructs an instance
     * @param channel to start processing
     */
    public ChannelStartProcessingRequest(Channel channel)
    {
        this(channel, null, null, null);
    }

    /**
     * Sets a flag to persistently attempt to start this channel.  When set to true, the channel processing manager
     * will repeatedly try to (re)start this channel while the application continues to run.
     * @param persistentAttempt if this start request should be continuously attempted until successful.
     */
    public void setPersistentAttempt(boolean persistentAttempt)
    {
        mPersistentAttempt = persistentAttempt;
    }

    /**
     * Indicates if this request should be persistently attempted until the channel processing is started.
     */
    public boolean isPersistentAttempt()
    {
        return mPersistentAttempt;
    }

    /**
     * Channel to start processing
     * @return
     */
    public Channel getChannel()
    {
        return mChannel;
    }

    /**
     * Optional traffic channel manager to (re)use.
     * @return traffic channel manager or null
     */
    public TrafficChannelManager getTrafficChannelManager()
    {
        return mTrafficChannelManager;
    }

    /**
     * Channel descriptor to use for the started channel
     */
    public IChannelDescriptor getChannelDescriptor()
    {
        return mChannelDescriptor;
    }

    /**
     * Indicates if this request has a non-null channel descriptor
     */
    public boolean hasChannelDescriptor()
    {
        return mChannelDescriptor != null;
    }

    /**
     * Identifier collection to use for preloading the channel with identifiers
     */
    public IdentifierCollection getIdentifierCollection()
    {
        return mIdentifierCollection;
    }

    /**
     * Indicates if this request has a non-null identifier collection
     */
    public boolean hasIdentifierCollection()
    {
        return mIdentifierCollection != null;
    }
}
