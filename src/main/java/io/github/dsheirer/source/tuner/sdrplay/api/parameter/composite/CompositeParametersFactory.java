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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.composite;

import io.github.dsheirer.source.tuner.sdrplay.api.Version;
import io.github.dsheirer.source.tuner.sdrplay.api.device.DeviceType;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * Creates a composite parameters instance for a device type
 */
public class CompositeParametersFactory
{
    /**
     * Creates a composite parameters instance for the specified device type
     * @param version of the api
     * @param deviceType to create
     * @param deviceParams sdrplay_api_DeviceParamsT structure
     * @param arena to allocate additional memory structures
     * @return instance
     */
    public static CompositeParameters create(Version version, DeviceType deviceType, MemorySegment deviceParams,
                                             Arena arena)
    {
        switch(deviceType)
        {
            case RSP1 -> {
                return new Rsp1CompositeParameters(version, deviceParams, arena);
            }
            case RSP1A -> {
                return new Rsp1aCompositeParameters(version, deviceParams, arena);
            }
            case RSP1B -> {
                return new Rsp1bCompositeParameters(version, deviceParams, arena);
            }
            case RSP2 -> {
                return new Rsp2CompositeParameters(version, deviceParams, arena);
            }
            case RSPduo -> {
                return new RspDuoCompositeParameters(version, deviceParams, arena);
            }
            case RSPdx, RSPdxR2 -> {
                return new RspDxCompositeParameters(version, deviceParams, arena);
            }
        }

        throw new IllegalArgumentException("Unrecognized device type: " + deviceType);
    }
}
