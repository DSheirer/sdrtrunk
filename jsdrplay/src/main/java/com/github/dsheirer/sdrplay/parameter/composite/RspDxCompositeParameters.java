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
import com.github.dsheirer.sdrplay.parameter.device.RspDxDeviceParameters;
import com.github.dsheirer.sdrplay.parameter.tuner.RspDxTunerParameters;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

/**
 * RSPdx Composite parameters (device and receiver)
 */
public class RspDxCompositeParameters extends CompositeParameters<RspDxDeviceParameters, RspDxTunerParameters>
{
    /**
     * Constructs an instance from the foreign memory segment
     *
     * @param memorySegment for the composite structure in foreign memory
     * @param memorySession for allocating additional memory segments for the sub-structures.
     */
    public RspDxCompositeParameters(MemorySegment memorySegment, MemorySession memorySession)
    {
        super(DeviceType.RSPdx, memorySegment, memorySession);
    }
}
