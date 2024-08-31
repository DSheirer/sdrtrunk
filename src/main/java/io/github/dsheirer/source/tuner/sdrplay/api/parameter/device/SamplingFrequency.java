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
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_FsFreqT;
import java.lang.foreign.MemorySegment;

/**
 * Sample Rate structure
 */
public class SamplingFrequency
{
    private final MemorySegment mMemorySegment;

    /**
     * Constructs an instance from a foreign memory segment and transfers the structure fields into this instance.
     * @param memorySegment pointer
     */
    public SamplingFrequency(MemorySegment memorySegment)
    {
        mMemorySegment = memorySegment;
    }

    private MemorySegment getMemorySegment()
    {
        return mMemorySegment;
    }

    /**
     * Sample rate frequency
     * @return frequency in Hertz
     */
    public double getSampleRate()
    {
        return sdrplay_api_FsFreqT.fsHz(getMemorySegment());
    }

    /**
     * Applies the requested sample rate to the device parameters
     * @param sampleRate requested rate
     * @param synchronousUpdate request
     * @param recalibrate request
     */
    public void setSampleRate(double sampleRate, boolean synchronousUpdate, boolean recalibrate)
    {
        sdrplay_api_FsFreqT.fsHz(getMemorySegment(), sampleRate);
        sdrplay_api_FsFreqT.syncUpdate(getMemorySegment(), synchronousUpdate ? Flag.TRUE : Flag.FALSE);
        sdrplay_api_FsFreqT.reCal(getMemorySegment(), recalibrate ? Flag.TRUE : Flag.FALSE);
    }

    /**
     * Applies the requested sample rate to the device parameters with synchronous update and recalibrate set to false.
     * @param sampleRate requested rate
     */
    public void setSampleRate(double sampleRate)
    {
        setSampleRate(sampleRate, false, false);
    }

    @Override
    public String toString()
    {
        return "Sample Rate:" + getSampleRate();
    }
}
