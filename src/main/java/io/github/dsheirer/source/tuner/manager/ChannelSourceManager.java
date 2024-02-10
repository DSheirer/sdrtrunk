/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import java.util.SortedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface to define the functionality of a channel source manager for handling tuner channel management and source
 * event listeners.
 */
public abstract class ChannelSourceManager implements ISourceEventProcessor
{
    private Logger mLogger = LoggerFactory.getLogger(ChannelSourceManager.class);

    private Broadcaster<SourceEvent> mSourceEventBroadcaster = new Broadcaster<>();

    /**
     * Prepare for disposal
     */
    public void dispose()
    {
        mSourceEventBroadcaster.clear();
    }

    /**
     * Sorted set of tuner channels being sourced by this source manager.  Set is ordered by frequency lowest to highest
     */
    public abstract SortedSet<TunerChannel> getTunerChannels();

    /**
     * State of this channel source manager for logging and debug purposes
     * @return description of the state or configuration of this manager
     */
    public abstract String getStateDescription();

    /**
     * Count of tuner channels being sourced by this source manager.
     * @return
     */
    public abstract int getTunerChannelCount();

    /**
     * Tells this channel source manager to shutdown all tuner channel sources to prepare for tuner shutdown.
     */
    public abstract void stopAllChannels();

    /**
     * Obtains a source for the tuner channel or returns null if the channel cannot be sourced by this tuner.
     *
     * Note: you MUST invoke start() on the obtained source to start the sample flow and invoke stop() to release all
     * resources allocated for the tuner channel source.
     *
     * @param tunerChannel for requested source
     * @param channelSpecification for the requested channel
     * @param threadName for the source thread pool
     * @return tuner channel source or null
     */
    public abstract TunerChannelSource getSource(TunerChannel tunerChannel, ChannelSpecification channelSpecification,
                                                 String threadName);

    /**
     * Signals that the complex buffer provider has an error and can no long provider buffers.  The subclass should
     * implement procedures to gracefully shutdown any tuner channel sources that have been provisioned.
     * @param errorMessage describing the error state
     */
    public abstract void setErrorMessage(String errorMessage);

    /**
     * Adds a listener to receive source events
     */
    public void addSourceEventListener(Listener<SourceEvent> listener)
    {
        mSourceEventBroadcaster.addListener(listener);
    }

    /**
     * Remove the listener from receiving source events
     */
    public void removeSourceEventListener(Listener<SourceEvent> listener)
    {
        mSourceEventBroadcaster.removeListener(listener);
    }

    /**
     * Broadcasts the source event to any registered listeners
     */
    protected void broadcast(SourceEvent sourceEvent)
    {
        mSourceEventBroadcaster.broadcast(sourceEvent);
    }
}
