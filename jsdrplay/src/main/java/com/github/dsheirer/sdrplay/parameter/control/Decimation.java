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

package com.github.dsheirer.sdrplay.parameter.control;

import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_DecimationT;
import com.github.dsheirer.sdrplay.util.Flag;

import java.lang.foreign.MemorySegment;

/**
 * Decimation structure (sdrplay_api_DecimationT)
 */
public class Decimation
{
    private MemorySegment mMemorySegment;

    /**
     * Constructs an instance from the foreign memory segment
     */
    public Decimation(MemorySegment memorySegment)
    {
        mMemorySegment = memorySegment;
    }

    /**
     * Foreign memory segment for this structure
     */
    public MemorySegment getMemorySegment()
    {
        return mMemorySegment;
    }

    /**
     * Indicates if decimation is enabled
     */
    public boolean isEnabled()
    {
        return Flag.evaluate(sdrplay_api_DecimationT.enable$get(getMemorySegment()));
    }

    /**
     * Enable or disable decimation
     */
    public void setEnabled(boolean enable)
    {
        sdrplay_api_DecimationT.enable$set(getMemorySegment(), Flag.of(enable));
    }

    /**
     * Decimation factor
     */
    public int getDecimationFactor()
    {
        return sdrplay_api_DecimationT.decimationFactor$get(getMemorySegment());
    }

    /**
     * Sets the decimation factor
     */
    public void setDecimationFactor(int decimationFactor)
    {
        sdrplay_api_DecimationT.decimationFactor$set(getMemorySegment(), (byte)decimationFactor);
    }

    /**
     * Indicates if the wideband signal setting is enabled
     */
    public boolean isWideBandSignal()
    {
        return Flag.evaluate(sdrplay_api_DecimationT.wideBandSignal$get(getMemorySegment()));
    }

    /**
     * Enables or disables the wideband signal setting
     */
    public void setWideBandSignal(boolean enable)
    {
        sdrplay_api_DecimationT.wideBandSignal$set(getMemorySegment(), Flag.of(enable));
    }
}
