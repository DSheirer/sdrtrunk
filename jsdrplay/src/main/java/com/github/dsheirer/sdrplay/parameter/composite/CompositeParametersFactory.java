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

package com.github.dsheirer.sdrplay.parameter.composite;

import com.github.dsheirer.sdrplay.device.DeviceType;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

/**
 * Creates a composite parameters instance for a device type
 */
public class CompositeParametersFactory
{
    /**
     * Creates a composite parameters instance for the specified device type
     * @param deviceType to create
     * @param memorySegment of foreign memory structure for the composite parameters
     * @param memorySession to allocate additional memory structures
     * @return instance
     */
    public static CompositeParameters create(DeviceType deviceType, MemorySegment memorySegment, MemorySession memorySession)
    {
        switch(deviceType)
        {
            case RSP1 -> {
                return new Rsp1CompositeParameters(memorySegment, memorySession);
            }
            case RSP1A -> {
                return new Rsp1aCompositeParameters(memorySegment, memorySession);
            }
            case RSP2 -> {
                return new Rsp2CompositeParameters(memorySegment, memorySession);
            }
            case RSPduo -> {
                return new RspDuoCompositeParameters(memorySegment, memorySession);
            }
            case RSPdx -> {
                return new RspDxCompositeParameters(memorySegment, memorySession);
            }
        }

        throw new IllegalArgumentException("Unrecognized device type: " + deviceType);
    }
}
