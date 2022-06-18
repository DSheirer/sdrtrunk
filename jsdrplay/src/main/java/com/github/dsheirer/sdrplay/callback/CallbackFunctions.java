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

package com.github.dsheirer.sdrplay.callback;

import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_CallbackFnsT;
import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_EventCallback_t;
import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_StreamCallback_t;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

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
     * @param memorySession for native memory allocation
     * @param deviceEventListener for device events
     * @param streamListener for streaming samples
     * @param streamCallbackListener for streaming events
     */
    public CallbackFunctions(MemorySession memorySession, IDeviceEventListener deviceEventListener,
                             IStreamListener streamListener, IStreamCallbackListener streamCallbackListener)
    {
        //Create the event callback function
        mDeviceEventAdapter = new DeviceEventAdapter(memorySession, deviceEventListener);
        MemorySegment eventFunction = sdrplay_api_EventCallback_t.allocate(mDeviceEventAdapter, memorySession);

        //Create the stream A callback function
        mStreamACallbackAdapter = new StreamCallbackAdapter(memorySession, streamListener, streamCallbackListener);
        MemorySegment streamAFunction = sdrplay_api_StreamCallback_t.allocate(mStreamACallbackAdapter, memorySession);

        //Create the callback functions union and populate the callback functions
        mCallbackFunctionsMemorySegment = sdrplay_api_CallbackFnsT.allocate(memorySession);
        sdrplay_api_CallbackFnsT.EventCbFn$set(mCallbackFunctionsMemorySegment, eventFunction.address());
        sdrplay_api_CallbackFnsT.StreamACbFn$set(mCallbackFunctionsMemorySegment, streamAFunction.address());
    }

    /**
     * Constructs a callback functions for dual-tuner use.
     * @param memorySession for native memory allocation
     * @param deviceEventListener for device events
     * @param streamAListener for streaming samples from the master tuner
     * @param streamBListener for streaming samples from the slave tuner
     * @param streamCallbackListener for streaming events
     */
    public CallbackFunctions(MemorySession memorySession, IDeviceEventListener deviceEventListener,
                             IStreamListener streamAListener, IStreamListener streamBListener,
                             IStreamCallbackListener streamCallbackListener)
    {
        //Create the event callback function
        mDeviceEventAdapter = new DeviceEventAdapter(memorySession, deviceEventListener);
        MemorySegment eventFunction = sdrplay_api_EventCallback_t.allocate(mDeviceEventAdapter, memorySession);

        //Create the stream A callback function
        mStreamACallbackAdapter = new StreamCallbackAdapter(memorySession, streamAListener, streamCallbackListener);
        MemorySegment streamAFunction = sdrplay_api_StreamCallback_t.allocate(mStreamACallbackAdapter, memorySession);

        //Create the stream B callback function
        mStreamBCallbackAdapter = new StreamCallbackAdapter(memorySession, streamBListener, streamCallbackListener);
        MemorySegment streamBFunction = sdrplay_api_StreamCallback_t.allocate(mStreamBCallbackAdapter, memorySession);

        //Create the callback functions union and populate the callback functions
        mCallbackFunctionsMemorySegment = sdrplay_api_CallbackFnsT.allocate(memorySession);
        sdrplay_api_CallbackFnsT.EventCbFn$set(mCallbackFunctionsMemorySegment, eventFunction.address());
        sdrplay_api_CallbackFnsT.StreamACbFn$set(mCallbackFunctionsMemorySegment, streamAFunction.address());
        sdrplay_api_CallbackFnsT.StreamBCbFn$set(mCallbackFunctionsMemorySegment, streamBFunction.address());
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
