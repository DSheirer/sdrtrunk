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

package com.github.dsheirer.sdrplay.parameter.tuner;

import com.github.dsheirer.sdrplay.SDRPlayException;
import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_RfFreqT;
import com.github.dsheirer.sdrplay.util.Flag;

import java.lang.foreign.MemorySegment;

/**
 * RF Frequency structure (sdrplay_api_RfFreqT)
 */
public class RfFrequency
{
    private static final double MINIMUM_FREQUENCY = 1_200;
    private static final double MAXIMUM_FREQUENCY = 2_000_000_000;
    private MemorySegment mMemorySegment;

    /**
     * Constructs an instance from the foreign memory segment
     * @param memorySegment representing the structure
     */
    public RfFrequency(MemorySegment memorySegment)
    {
        mMemorySegment = memorySegment;
    }

    private MemorySegment getMemorySegment()
    {
        return mMemorySegment;
    }

    /**
     * RF Frequency
     * @return frequency in Hertz
     */
    public double getFrequency()
    {
        return sdrplay_api_RfFreqT.rfHz$get(getMemorySegment());
    }

    /**
     * Sets the RF frequency
     * @param frequency (0 < frequency <= 2,000,000,000 Hz)
     * @param synchronousUpdate to set the frequency as a synchronous update
     * @throws SDRPlayException for an invalid frequency (ie out of valid range)
     */
    public void setFrequency(double frequency, boolean synchronousUpdate) throws SDRPlayException
    {
        if(MINIMUM_FREQUENCY < frequency && frequency <= MAXIMUM_FREQUENCY)
        {
            sdrplay_api_RfFreqT.rfHz$set(getMemorySegment(), frequency);
            sdrplay_api_RfFreqT.syncUpdate$set(getMemorySegment(), Flag.of(synchronousUpdate));
        }
        else
        {
            throw new SDRPlayException("Invalid frequency: " + frequency + " - valid range: " +
                    MINIMUM_FREQUENCY + "-" + MAXIMUM_FREQUENCY);
        }
    }

    /**
     * Sets the RF frequency as a synchronous update
     */
    public void setFrequency(double frequency) throws SDRPlayException
    {
        setFrequency(frequency, true);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Frequency:").append(getFrequency());
        return sb.toString();
    }
}
