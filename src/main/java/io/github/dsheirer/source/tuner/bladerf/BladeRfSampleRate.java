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
 * Sample rates offered for bladeRF tuners.  Each is a multiple of the 25 kHz polyphase channel spacing.
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
    RATE_30_0(30_000_000, "30.00 MHz");

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

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Sample rates within the device maximum, which is the lower of the hardware limit and what the USB link sustains.
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
