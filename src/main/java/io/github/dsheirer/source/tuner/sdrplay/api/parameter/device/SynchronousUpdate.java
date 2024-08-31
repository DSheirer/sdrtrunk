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

import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_SyncUpdateT;
import java.lang.foreign.MemorySegment;

/**
 * Sync Update structure
 */
public class SynchronousUpdate
{
    private final MemorySegment mMemorySegment;

    /**
     * Constructs an instance from a foreign memory segment
     * @param memorySegment pointer
     */
    public SynchronousUpdate(MemorySegment memorySegment)
    {
        mMemorySegment = memorySegment;
    }

    private MemorySegment getMemorySegment()
    {
        return mMemorySegment;
    }

    /**
     * Sets the sample number and period to apply to both tuners for a synchronous update.
     * @param sampleNumber value
     * @param period value
     */
    public void set(int sampleNumber, int period)
    {
        sdrplay_api_SyncUpdateT.sampleNum(getMemorySegment(), sampleNumber);
        sdrplay_api_SyncUpdateT.period(getMemorySegment(), period);
    }
}
