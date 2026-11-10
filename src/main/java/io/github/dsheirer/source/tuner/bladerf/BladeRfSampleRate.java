/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.bladerf;

import java.util.ArrayList;
import java.util.List;

/**
 * Sample rates offered for bladeRF tuners, up to the 122.88 MHz maximum of the bladeRF 2.0.  Use getRates() to get the
 * rates supported by a specific device, since the bladeRF 1.0, USB 2.0 connections and older FPGA images have lower
 * maximums.
 *
 * Rates above 61.44 MHz use the AD9361 oversample feature, which requires 8-bit samples.
 *
 * Each rate is a multiple of the 25 kHz polyphase channel spacing, except for the 61.44 and 122.88 MHz rates.  The
 * channelizer uses slightly wider channels at those rates.
 */
public enum BladeRfSampleRate
{
    RATE_1_25(1_250_000, "1.25 MHz"),
    RATE_2_5(2_500_000, "2.50 MHz"),
    RATE_5_0(5_000_000, "5.00 MHz"),
    RATE_7_5(7_500_000, "7.50 MHz"),
    RATE_10_0(10_000_000, "10.00 MHz"),
    RATE_12_5(12_500_000, "12.50 MHz"),
    RATE_15_0(15_000_000, "15.00 MHz"),
    RATE_20_0(20_000_000, "20.00 MHz"),
    RATE_25_0(25_000_000, "25.00 MHz"),
    RATE_30_0(30_000_000, "30.00 MHz"),
    RATE_35_0(35_000_000, "35.00 MHz"),
    RATE_40_0(40_000_000, "40.00 MHz"),
    RATE_45_0(45_000_000, "45.00 MHz"),
    RATE_50_0(50_000_000, "50.00 MHz"),
    RATE_55_0(55_000_000, "55.00 MHz"),
    RATE_60_0(60_000_000, "60.00 MHz"),
    RATE_61_44(61_440_000, "61.44 MHz"),
    RATE_70_0(70_000_000, "70.00 MHz (8-bit)"),
    RATE_80_0(80_000_000, "80.00 MHz (8-bit)"),
    RATE_90_0(90_000_000, "90.00 MHz (8-bit)"),
    RATE_100_0(100_000_000, "100.00 MHz (8-bit)"),
    RATE_110_0(110_000_000, "110.00 MHz (8-bit)"),
    RATE_120_0(120_000_000, "120.00 MHz (8-bit)"),
    RATE_122_88(122_880_000, "122.88 MHz (8-bit)");

    public static final BladeRfSampleRate DEFAULT = RATE_10_0;
    private final int mRate;
    private final String mLabel;

    BladeRfSampleRate(int rate, String label)
    {
        mRate = rate;
        mLabel = label;
    }

    /**
     * Sample rate in Hertz
     */
    public int getRate()
    {
        return mRate;
    }

    /**
     * Indicates if this rate requires the oversample feature and 8-bit samples.
     */
    public boolean isOversampled()
    {
        return mRate > RATE_61_44.mRate;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Sample rates within the device maximum, which is the lower of what the hardware, FPGA image and USB link support.
     * @param maximumRate in Hertz
     * @return rates, lowest to highest
     */
    public static List<BladeRfSampleRate> getRates(long maximumRate)
    {
        List<BladeRfSampleRate> rates = new ArrayList<>();

        for(BladeRfSampleRate rate : values())
        {
            if(rate.getRate() <= maximumRate)
            {
                rates.add(rate);
            }
        }

        return rates;
    }

    /**
     * Finds the highest rate from the list that doesn't exceed the requested rate.
     * @param rates to search, lowest to highest
     * @param requested rate in Hertz
     * @return closest rate, or the lowest rate when the requested rate is below all of them.
     */
    public static BladeRfSampleRate getClosest(List<BladeRfSampleRate> rates, int requested)
    {
        BladeRfSampleRate closest = rates.getFirst();

        for(BladeRfSampleRate rate : rates)
        {
            if(rate.getRate() <= requested)
            {
                closest = rate;
            }
        }

        return closest;
    }
}
