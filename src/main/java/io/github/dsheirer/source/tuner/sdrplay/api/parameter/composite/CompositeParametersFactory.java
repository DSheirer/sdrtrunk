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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.composite;

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
     * @param deviceType to create
     * @param memorySegment of foreign memory structure for the composite parameters
     * @param arena to allocate additional memory structures
     * @return instance
     */
    public static CompositeParameters create(DeviceType deviceType, MemorySegment memorySegment, Arena arena)
    {
        switch(deviceType)
        {
            case RSP1 -> {
                return new Rsp1CompositeParameters(memorySegment, arena);
            }
            case RSP1A -> {
                return new Rsp1aCompositeParameters(memorySegment, arena);
            }
            case RSP2 -> {
                return new Rsp2CompositeParameters(memorySegment, arena);
            }
            case RSPduo -> {
                return new RspDuoCompositeParameters(memorySegment, arena);
            }
            case RSPdx -> {
                return new RspDxCompositeParameters(memorySegment, arena);
            }
        }

        throw new IllegalArgumentException("Unrecognized device type: " + deviceType);
    }
}
