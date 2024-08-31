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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.device;

import io.github.dsheirer.source.tuner.sdrplay.api.device.DeviceType;
import java.lang.foreign.MemorySegment;

/**
 * Creates device parameters instance from a foreign memory segment
 */
public class DeviceParametersFactory
{
    /**
     * Create a device parameters instance
     * @param deviceType for the device
     * @param memorySegment for an allocated sdrplay_api_DevParamsT structure
     * @return instance
     */
    public static DeviceParameters create(DeviceType deviceType, MemorySegment memorySegment)
    {
        switch(deviceType)
        {
            case RSP1 -> {
                return new Rsp1DeviceParameters(memorySegment);
            }
            //RSP1A and RSP1B share the same device parameters structures
            case RSP1A, RSP1B -> {
                return new Rsp1aDeviceParameters(memorySegment);
            }
            case RSP2 -> {
                return new Rsp2DeviceParameters(memorySegment);
            }
            case RSPduo -> {
                return new RspDuoDeviceParameters(memorySegment);
            }
            case RSPdx, RSPdxR2 -> {
                return new RspDxDeviceParameters(memorySegment);
            }
        }

        throw new IllegalArgumentException("Unrecognized Device Type: " + deviceType);
    }
}
