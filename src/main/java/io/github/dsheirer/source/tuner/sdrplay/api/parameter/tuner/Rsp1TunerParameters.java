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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner;

import java.lang.foreign.MemorySegment;

/**
 * RSP1 Tuner Parameters structure
 *
 * Note: the RSP1 doesn't have any additional settings beyond the basic tuner parameters
 */
public class Rsp1TunerParameters extends TunerParameters
{
    /**
     * Constructs an instance from the foreign memory segment.
     */
    public Rsp1TunerParameters(MemorySegment tunerParametersMemorySegment)
    {
        super(tunerParametersMemorySegment);
    }
}
