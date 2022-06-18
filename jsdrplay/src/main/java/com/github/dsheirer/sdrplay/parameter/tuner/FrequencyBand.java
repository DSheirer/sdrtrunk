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

import java.util.EnumSet;

/**
 * RSP Frequency Bands
 */
public enum FrequencyBand
{
    BAND_0_12(1_000, 12_000_000, "0.001-12 MHz"),
    BAND_12_60(12_000_000, 60_000_000, "12-60 MHz"),
    BAND_60_250(60_000_000, 250_000_000, "60-250 MHz"),
    BAND_250_420(250_000_000, 420_000_000, "250-420 MHz"),
    BAND_420_1000(420_000_000, 1_000_000_000, "420-1000 MHz"),
    BAND_1000_2000(1_000_000_000, 2_000_000_000, "1000-2000 MHz"),
    UNKNOWN(-1, 0,"UNKNOWN");

    private long mMinimum;
    private long mMaximum;
    private String mDescription;

    FrequencyBand(long min, long max, String description)
    {
        mMinimum = min;
        mMaximum = max;
        mDescription = description;
    }

    /**
     * Valid frequency bands
     */
    public static EnumSet<FrequencyBand> VALID_FREQUENCY_BANDS = EnumSet.range(BAND_0_12, BAND_1000_2000);

    /**
     * Minimum frequency
     * @return frequency in Hertz
     */
    public long getMinimum()
    {
        return mMinimum;
    }

    /**
     * Maximum frequency
     * @return frequency in Hertz
     */
    public long getMaximum()
    {
        return mMaximum;
    }

    /**
     * Lookup the entry from a return code
     * @param frequency to lookup
     * @return entry or UNKNOWN if the code is not recognized
     */
    public static FrequencyBand fromValue(long frequency)
    {
        for(FrequencyBand band: FrequencyBand.values())
        {
            if(band.contains(frequency))
            {
                return band;
            }
        }
        
        return UNKNOWN;
    }

    /**
     * Indicates if this frequency band entry contains the specified frequency
     * @param frequency to check
     * @return true if this entry contains the frequency
     */
    public boolean contains(long frequency)
    {
        return getMinimum() <= frequency && frequency < getMaximum();
    }

    @Override
    public String toString()
    {
        return mDescription;
    }
}
