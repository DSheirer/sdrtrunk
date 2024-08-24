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

package io.github.dsheirer.source.tuner.sdrplay.api.callback;

import io.github.dsheirer.source.tuner.sdrplay.api.device.TunerSelect;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.event.EventParametersFactory;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.event.EventType;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_EventCallback_t;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_EventParamsT;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for Device Event callbacks.  Implements foreign interface and transfers foreign memory event details to
 * native Java and invokes the appropriate interface methods for an event listener.
 */
public class DeviceEventAdapter implements sdrplay_api_EventCallback_t.Function
{
    private static final Logger mLog = LoggerFactory.getLogger(DeviceEventAdapter.class);
    private IDeviceEventListener mDeviceEventListener;

    /**
     * Constructs an instance.
     * @param listener to receive translated device events.
     */
    public DeviceEventAdapter(IDeviceEventListener listener)
    {
        setListener(listener);
    }

    /**
     * Updates the device event listener
     * @param listener to receive device events
     */
    public void setListener(IDeviceEventListener listener)
    {
        if(listener == null)
        {
            throw new IllegalArgumentException("Device event listener must be non-null");
        }

        mDeviceEventListener = listener;
    }

    @Override
    public void apply(int eventTypeId, int tunerSelectId, MemorySegment eventParametersPointer,
                      MemorySegment callbackContext)
    {
        //Use an auto arena to allow the garbage collector to deallocate, since we don't know if the device event
        //listener will ever hold onto any object that is backed by our memory segment.
        MemorySegment eventParams = eventParametersPointer.reinterpret(sdrplay_api_EventParamsT.sizeof(), Arena.ofAuto(), null);
        EventType eventType = EventType.fromValue(eventTypeId);
        TunerSelect tunerSelect = TunerSelect.fromValue(tunerSelectId);

        switch(eventType)
        {
            case GAIN_CHANGE ->
            {
                mDeviceEventListener.processGainChange(tunerSelect,
                        EventParametersFactory.createGainCallbackParameters(eventParams));
            }
            case POWER_OVERLOAD_CHANGE ->
            {
                mDeviceEventListener.processPowerOverload(tunerSelect,
                        EventParametersFactory.createPowerOverloadCallbackParameters(eventParams));
            }
            case DEVICE_REMOVED ->
            {
                mDeviceEventListener.processDeviceRemoval(tunerSelect);
            }
            case RSP_DUO_MODE_CHANGE ->
            {
                mDeviceEventListener.processRspDuoModeChange(tunerSelect,
                        EventParametersFactory.createRspDuoModeCallbackParameters(eventParams));
            }
            case UNKNOWN ->
            {
                mLog.warn("Unknown device event callback ignored.  Please contact the library developer as this may " +
                        "indicate a change to the SDRPlay API change. Tuner:" + tunerSelect + " Event Type ID:" +
                        eventTypeId);
                mDeviceEventListener.processEvent(eventType, tunerSelect);
            }
            default ->
            {
                throw new IllegalStateException("DeviceEventAdapter must be updated handle EventType." + eventType);
            }
        }
    }
}
