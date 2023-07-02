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
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.device.RspDxDeviceParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.RspDxTunerParameters;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * RSPdx Composite parameters (device and receiver)
 */
public class RspDxCompositeParameters extends CompositeParameters<RspDxDeviceParameters, RspDxTunerParameters>
{
    /**
     * Constructs an instance from the foreign memory segment
     *
     * @param memorySegment for the composite structure in foreign memory
     * @param arena for allocating additional memory segments for the sub-structures.
     */
    public RspDxCompositeParameters(MemorySegment memorySegment, Arena arena)
    {
        super(DeviceType.RSPdx, memorySegment, arena);
    }
}
