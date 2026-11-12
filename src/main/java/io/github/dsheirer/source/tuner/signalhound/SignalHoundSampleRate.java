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

package io.github.dsheirer.source.tuner.signalhound;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Signal Hound I/Q sample rate: the family's base sample rate divided by a power of two decimation.
 * @param decimation power of two
 * @param rate in Hertz
 */
public record SignalHoundSampleRate(int decimation, int rate)
{
    //Decimations offered to the user.  Higher decimations are supported by the APIs, but the sample rates are below
    //what is useful for channelizing (600-780 kHz at the highest decimation offered here).
    public static final int MAXIMUM_DECIMATION = 64;
    public static final int DEFAULT_RATE = 10_000_000;
    private static final DecimalFormat FORMAT = new DecimalFormat("0.00###");

    /**
     * Sample rates of a family, highest rate first.  Every rate is an integer number of Hertz.
     */
    public static List<SignalHoundSampleRate> getRates(SignalHoundFamily family)
    {
        List<SignalHoundSampleRate> rates = new ArrayList<>();

        for(int decimation = 1; decimation <= MAXIMUM_DECIMATION; decimation *= 2)
        {
            rates.add(new SignalHoundSampleRate(decimation, family.getBaseSampleRate() / decimation));
        }

        return rates;
    }

    /**
     * Finds the highest rate that doesn't exceed the requested rate.
     * @param rates to search
     * @param requested rate in Hertz
     * @return closest rate, or the lowest rate when the requested rate is below all of them
     */
    public static SignalHoundSampleRate getClosest(List<SignalHoundSampleRate> rates, int requested)
    {
        SignalHoundSampleRate closest = null;
        SignalHoundSampleRate lowest = null;

        for(SignalHoundSampleRate rate : rates)
        {
            if(rate.rate() <= requested && (closest == null || rate.rate() > closest.rate()))
            {
                closest = rate;
            }

            if(lowest == null || rate.rate() < lowest.rate())
            {
                lowest = rate;
            }
        }

        return closest != null ? closest : lowest;
    }

    @Override
    public String toString()
    {
        return FORMAT.format(rate / 1E6) + " MHz";
    }
}
