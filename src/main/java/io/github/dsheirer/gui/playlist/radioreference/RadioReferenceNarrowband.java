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

package io.github.dsheirer.gui.playlist.radioreference;

/**
 * Narrowband (12.5 kHz) correction for conventional analog FM frequencies imported from Radio Reference.
 *
 * Radio Reference lists many frequencies with the wideband FM mode that have been narrowband since the FCC Part 90
 * narrowbanding mandate (January 1, 2013) for land mobile frequencies in the 150-174 MHz and 421-512 MHz bands, or
 * since the NTIA narrowbanding of federal VHF/UHF frequencies.  Frequencies in those bands are created as 12.5 kHz
 * channels, except for services that are still licensed for wideband (25 kHz) operation:
 * <ul>
 *     <li>VHF marine (Part 80) ship and coast frequencies: 156.000-157.450 MHz and 161.575-162.025 MHz</li>
 *     <li>NOAA weather radio broadcasts: 162.400-162.550 MHz</li>
 *     <li>MURS wideband channels (Part 95): 154.570 and 154.600 MHz</li>
 *     <li>GMRS (Part 95) main and repeater input channels: 462.550-462.725 MHz and 467.550-467.725 MHz on 25 kHz
 *     steps (the interstitial FRS/GMRS channels between them are narrowband)</li>
 *     <li>Amateur (Part 97) 420-450 MHz band, which is outside the corrected UHF range</li>
 * </ul>
 * 800 and 900 MHz frequencies aren't covered by the mandate and keep the wideband mode.
 */
public final class RadioReferenceNarrowband
{
    private static final long MHZ = 1_000_000L;
    private static final long KHZ = 1_000L;

    private static final long VHF_START = 150 * MHZ;
    private static final long VHF_END = 174 * MHZ;
    private static final long UHF_START = 450 * MHZ;
    private static final long UHF_END = 512 * MHZ;

    /**
     * Wideband exceptions as {start, end} in Hertz, inclusive.  Exact channels are listed with a 1 kHz tolerance.
     */
    private static final long[][] WIDEBAND_EXCEPTIONS = {
        {156_000 * KHZ, 157_450 * KHZ},                  //VHF marine ship and coast
        {161_575 * KHZ, 162_025 * KHZ},                  //VHF marine coast, AIS
        {162_400 * KHZ, 162_550 * KHZ},                  //NOAA weather radio
        {154_569 * KHZ, 154_571 * KHZ},                  //MURS 154.570 (Blue Dot)
        {154_599 * KHZ, 154_601 * KHZ},                  //MURS 154.600 (Green Dot)
    };

    /**
     * GMRS main channels (462.550-462.725 MHz) and repeater inputs (467.550-467.725 MHz) on 25 kHz steps.  The
     * interstitial channels between them (e.g. 462.5625 MHz) are narrowband FRS/GMRS channels.
     */
    private static final long[] GMRS_WIDEBAND_START = {462_550 * KHZ, 467_550 * KHZ};
    private static final long GMRS_WIDEBAND_SPAN = 175 * KHZ;
    private static final long GMRS_CHANNEL_STEP = 25 * KHZ;
    private static final long CHANNEL_TOLERANCE = 1 * KHZ;

    private RadioReferenceNarrowband()
    {
    }

    /**
     * Indicates if an analog FM frequency is required to be narrowband (12.5 kHz).
     * @param frequency in Hertz
     * @return true if the frequency is in the 150-174 MHz or 450-512 MHz bands and isn't a wideband exception
     */
    public static boolean isNarrowband(long frequency)
    {
        boolean inBand = (frequency >= VHF_START && frequency <= VHF_END) ||
            (frequency >= UHF_START && frequency <= UHF_END);

        if(!inBand)
        {
            return false;
        }

        for(long[] exception : WIDEBAND_EXCEPTIONS)
        {
            if(frequency >= exception[0] && frequency <= exception[1])
            {
                return false;
            }
        }

        for(long start : GMRS_WIDEBAND_START)
        {
            long offset = frequency - start;

            if(offset >= -CHANNEL_TOLERANCE && offset <= GMRS_WIDEBAND_SPAN + CHANNEL_TOLERANCE)
            {
                long fromChannel = Math.floorMod(offset + CHANNEL_TOLERANCE, GMRS_CHANNEL_STEP);

                if(fromChannel <= 2 * CHANNEL_TOLERANCE)
                {
                    return false;
                }
            }
        }

        return true;
    }
}
