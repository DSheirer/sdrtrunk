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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner;

import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_GainValuesT;
import java.lang.foreign.MemorySegment;

/**
 * Gain Values structure (sdrplay_api_GainValuesT)
 *
 * Note: these are output parameters, so there are no setter methods.
 */
public class GainValues
{
    private final MemorySegment mMemorySegment;

    /**
     * Constructs an instance from the foreign memory segment
     */
    public GainValues(MemorySegment memorySegment)
    {
        mMemorySegment = memorySegment;
    }

    /**
     * Foreign memory segment for this structure
     */
    private MemorySegment getMemorySegment()
    {
        return mMemorySegment;
    }


    public float getCurrent()
    {
        return sdrplay_api_GainValuesT.curr(getMemorySegment());
    }

    public float getMax()
    {
        return sdrplay_api_GainValuesT.max(getMemorySegment());
    }

    public float getMin()
    {
        return sdrplay_api_GainValuesT.min(getMemorySegment());
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Current:").append(getCurrent()).append(" Max:").append(getMax()).append(" Min:").append(getMin());
        return sb.toString();
    }
}
