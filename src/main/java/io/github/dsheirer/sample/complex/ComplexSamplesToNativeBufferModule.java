/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple module that can be added live to a processing chain to tap into the stream of ComplexSamples buffers and
 * convert them to INativeBuffers for relay to a registered native buffer listener.
 */
public class ComplexSamplesToNativeBufferModule extends Module implements IComplexSamplesListener, Listener<ComplexSamples>
{
    private static final Logger mLog = LoggerFactory.getLogger(ComplexSamplesToNativeBufferModule.class);
    private Listener<INativeBuffer> mNativeBufferListener;

    public ComplexSamplesToNativeBufferModule()
    {
    }

    @Override
    public Listener<ComplexSamples> getComplexSamplesListener()
    {
        return this;
    }

    /**
     * Primary receive method to process complex samples buffer by wrapping with a native buffer adapter and sending
     * to the registered listener.
     *
     * Note: this method is synchronized to ensure that multiple calling threads don't step on each other.
     *
     * @param complexSamples to process and relay.
     */
    @Override
    public synchronized void receive(ComplexSamples complexSamples)
    {
        if(mNativeBufferListener != null)
        {
            mNativeBufferListener.receive(new ComplexSamplesNativeBufferAdapter(complexSamples));
        }
    }

    /**
     * Registers the listener to receive native buffers.
     * @param listener to register
     */
    public void setListener(Listener<INativeBuffer> listener)
    {
        mNativeBufferListener = listener;
    }

    /**
     * Unregisters a listener from receiving native buffers.
     */
    public void removeListener()
    {
        mNativeBufferListener = null;
    }

    @Override
    public void reset() {}

    @Override
    public void start() {}

    @Override
    public void stop() {}

}
