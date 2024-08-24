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

import java.lang.foreign.MemorySegment;

/**
 * RSP-1 Device Parameters structure
 */
public class Rsp1DeviceParameters extends DeviceParameters
{
    /**
     * Constructs an instance
     * @param devParams for an allocated sdrplay_api_DevParamsT structure
     */
    public Rsp1DeviceParameters(MemorySegment devParams)
    {
        super(devParams);
    }
}
