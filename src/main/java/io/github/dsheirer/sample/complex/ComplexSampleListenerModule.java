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

package io.github.dsheirer.sample.complex;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module that receives and rebroadcasts complex samples.
 */
public class ComplexSampleListenerModule extends Module implements IComplexSamplesListener, Listener<ComplexSamples>
{
    private static final Logger mLog = LoggerFactory.getLogger(ComplexSampleListenerModule.class);
    private Broadcaster<INativeBuffer> mBroadcaster = new Broadcaster<>();

    /**
     * Implements the interface
     * @return this.
     */
    @Override
    public Listener<ComplexSamples> getComplexSamplesListener()
    {
        return this;
    }

    /**
     * Primary receive method for sample buffers to rebroadcast.
     * @param complexSamples to rebroadcast
     */
    @Override
    public void receive(ComplexSamples complexSamples)
    {
        mBroadcaster.broadcast(new ComplexSamplesNativeBufferAdapter(complexSamples));
    }

    /**
     * Adds listener to receive rebroadcast of complex samples buffer.
     * @param listener to add
     */
    public void addListener(Listener<INativeBuffer> listener)
    {
        mBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving rebroadcast of complex samples.
     * @param listener to remove
     */
    public void removeListener(Listener<INativeBuffer> listener)
    {
        mBroadcaster.removeListener(listener);
    }

    @Override
    public void reset()
    {
        //Not implemented
    }

    @Override
    public void start()
    {
        //Not implemented
    }

    @Override
    public void stop()
    {
        //Not implemented
    }
}
