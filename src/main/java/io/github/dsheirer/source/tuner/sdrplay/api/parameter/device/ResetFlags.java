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

import io.github.dsheirer.source.tuner.sdrplay.api.util.Flag;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_ResetFlagsT;
import java.lang.foreign.MemorySegment;

/**
 * Reset Flags structure
 */
public class ResetFlags
{
    private final MemorySegment mMemorySegment;

    /**
     * Constructs an instance from a foreign memory segment and transfers the structure fields into this instance.
     * @param memorySegment pointer
     */
    public ResetFlags(MemorySegment memorySegment)
    {
        mMemorySegment = memorySegment;
    }

    /**
     * Foreign memory segment
     */
    private MemorySegment getMemorySegment()
    {
        return mMemorySegment;
    }

    /**
     * Request a reset of the gain reduction
     */
    public void resetGain(boolean reset)
    {
        sdrplay_api_ResetFlagsT.resetGainUpdate(getMemorySegment(), reset ? Flag.TRUE : Flag.FALSE);
    }

    /**
     * Request a reset of the center frequency value
     */
    public void resetFrequency(boolean reset)
    {
        sdrplay_api_ResetFlagsT.resetRfUpdate(getMemorySegment(), reset ? Flag.TRUE : Flag.FALSE);
    }

    /**
     * Request a reset of the sample rate value.
     */
    public void resetSampleRate(boolean reset)
    {
        sdrplay_api_ResetFlagsT.resetFsUpdate(getMemorySegment(), reset ? Flag.TRUE : Flag.FALSE);
    }
}
