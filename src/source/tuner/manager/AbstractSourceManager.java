/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package source.tuner.manager;

import sample.Broadcaster;
import sample.Listener;
import sample.complex.ComplexBuffer;
import sample.complex.IComplexBufferProvider;
import source.SourceEvent;
import source.tuner.channel.TunerChannel;
import source.tuner.channel.TunerChannelSource;

import java.util.List;

/**
 * Interface to define the functionality of a source manager for handling tuner channel management, complex buffer
 * listeners, and source event listeners.
 */
public abstract class AbstractSourceManager implements IComplexBufferProvider, Listener<SourceEvent>
{
    protected Broadcaster<ComplexBuffer> mComplexBufferBroadcaster = new Broadcaster<>();

    /**
     * Indicates if this source manager can tune (ie provide a source for) the specified tuner channel
     */
    public abstract boolean canTune(TunerChannel tunerChannel);

    /**
     * List of tuner channels being sourced by this source manager
     */
    public abstract List<TunerChannel> getChannels();

    /**
     * Obtains a source for the tuner channel or returns null if the channel cannot be sourced by this tuner.
     * Note: use the canTune() method to check if a tuner channel can be sourced/tuned prior to accessing this method.
     *
     * @param tunerChannel for requested source
     * @return tuner channel source or null
     */
    public abstract TunerChannelSource getSource(TunerChannel tunerChannel);


    /**
     * Releases the source and all related resources
     */
    public abstract void releaseSource(TunerChannelSource source);

    /**
     * Adds the listener to receive source events from this source manager
     *
     * @param sourceEventListener to add
     */
    public abstract void addSourceEventListener(Listener<SourceEvent> sourceEventListener);

    /**
     * Removes the listener from receiving source events from this source manager
     * @param sourceEventListener to remove
     */
    public abstract void removeSourceEventListener(Listener<SourceEvent> sourceEventListener);

    /**
     * Adds the listener to receive complex buffer samples
     */
    @Override
    public void addComplexBufferListener(Listener<ComplexBuffer> listener)
    {
        mComplexBufferBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving complex buffer samples
     */
    @Override
    public void removeComplexBufferListener(Listener<ComplexBuffer> listener)
    {
        mComplexBufferBroadcaster.removeListener(listener);
    }

    /**
     * Indicates if there are any complex buffer listeners registered with this source manager
     */
    @Override
    public boolean hasComplexBufferListeners()
    {
        return mComplexBufferBroadcaster.hasListeners();
    }

    /**
     * Broadcasts the buffer to any registered listeners
     */
    protected void broadcast(ComplexBuffer complexBuffer)
    {
        mComplexBufferBroadcaster.broadcast(complexBuffer);
    }
}
