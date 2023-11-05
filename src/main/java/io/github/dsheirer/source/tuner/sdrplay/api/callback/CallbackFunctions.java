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

package io.github.dsheirer.source.tuner.sdrplay.api.callback;

import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_CallbackFnsT;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_EventCallback_t;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_StreamCallback_t;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * Callback functions (sdrplay_api_CallbackFnsT) factory
 */
public class CallbackFunctions
{
    private DeviceEventAdapter mDeviceEventAdapter;
    private StreamCallbackAdapter mStreamACallbackAdapter;
    private StreamCallbackAdapter mStreamBCallbackAdapter;
    private MemorySegment mCallbackFunctionsMemorySegment;

    /**
     * Constructs a callback functions for single-tuner use.
     * @param longLifecycleArena for native memory allocation for long lifecycle objects.
     * @param deviceEventListener for device events
     * @param streamListener for streaming samples
     * @param streamCallbackListener for streaming events
     */
    public CallbackFunctions(Arena longLifecycleArena, IDeviceEventListener deviceEventListener,
                             IStreamListener streamListener, IStreamCallbackListener streamCallbackListener)
    {
        //Create the event callback function
        mDeviceEventAdapter = new DeviceEventAdapter(deviceEventListener);
        MemorySegment eventFunction = sdrplay_api_EventCallback_t.allocate(mDeviceEventAdapter, longLifecycleArena.scope());

        //Create the stream A callback function
        mStreamACallbackAdapter = new StreamCallbackAdapter(streamListener, streamCallbackListener);
        MemorySegment streamAFunction = sdrplay_api_StreamCallback_t.allocate(mStreamACallbackAdapter, longLifecycleArena.scope());

        //Create the callback functions union and populate the callback functions
        mCallbackFunctionsMemorySegment = sdrplay_api_CallbackFnsT.allocate(longLifecycleArena);
        sdrplay_api_CallbackFnsT.EventCbFn$set(mCallbackFunctionsMemorySegment, eventFunction);
        sdrplay_api_CallbackFnsT.StreamACbFn$set(mCallbackFunctionsMemorySegment, streamAFunction);
    }

    /**
     * Constructs a callback functions for dual-tuner use.
     * @param arena for native memory allocation
     * @param deviceEventListener for device events
     * @param streamAListener for streaming samples from the master tuner
     * @param streamBListener for streaming samples from the slave tuner
     * @param streamCallbackListener for streaming events
     */
    public CallbackFunctions(Arena arena, IDeviceEventListener deviceEventListener,
                             IStreamListener streamAListener, IStreamListener streamBListener,
                             IStreamCallbackListener streamCallbackListener)
    {
        //Create the event callback function
        mDeviceEventAdapter = new DeviceEventAdapter(deviceEventListener);
        MemorySegment eventFunction = sdrplay_api_EventCallback_t.allocate(mDeviceEventAdapter, arena.scope());

        //Create the stream A callback function
        mStreamACallbackAdapter = new StreamCallbackAdapter(streamAListener, streamCallbackListener);
        MemorySegment streamAFunction = sdrplay_api_StreamCallback_t.allocate(mStreamACallbackAdapter, arena.scope());

        //Create the stream B callback function
        mStreamBCallbackAdapter = new StreamCallbackAdapter(streamBListener, streamCallbackListener);
        MemorySegment streamBFunction = sdrplay_api_StreamCallback_t.allocate(mStreamBCallbackAdapter, arena.scope());

        //Create the callback functions union and populate the callback functions
        mCallbackFunctionsMemorySegment = sdrplay_api_CallbackFnsT.allocate(arena);
        sdrplay_api_CallbackFnsT.EventCbFn$set(mCallbackFunctionsMemorySegment, eventFunction);
        sdrplay_api_CallbackFnsT.StreamACbFn$set(mCallbackFunctionsMemorySegment, streamAFunction);
        sdrplay_api_CallbackFnsT.StreamBCbFn$set(mCallbackFunctionsMemorySegment, streamBFunction);
    }

    /**
     * Foreign memory segment for the callback functions.
     */
    public MemorySegment getCallbackFunctionsMemorySegment()
    {
        return mCallbackFunctionsMemorySegment;
    }

    /**
     * Updates the device event listener
     * @param listener to receive device events
     */
    public void setDeviceEventListener(IDeviceEventListener listener)
    {
        mDeviceEventAdapter.setListener(listener);
    }

    /**
     * Updates the stream A listener
     * @param listener to receive samples for stream A
     */
    public void setStreamAListener(IStreamListener listener)
    {
        mStreamACallbackAdapter.setListener(listener);
    }

    /**
     * Updates the stream B listener
     * @param listener to receive samples for stream B
     */
    public void setStreamBListener(IStreamListener listener)
    {
        mStreamBCallbackAdapter.setListener(listener);
    }
}
