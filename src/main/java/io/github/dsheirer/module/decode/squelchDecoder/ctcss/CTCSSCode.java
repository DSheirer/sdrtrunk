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

package io.github.dsheirer.module.decode.squelchDecoder.ctcss;

import java.util.EnumSet;
import java.util.Set;

/**
 * Standard CTCSS (Continuous Tone-Coded Squelch System) tones.
 * Also known as PL (Private Line) tones (Motorola trademark).
 *
 * Frequencies and EIA/TIA standard designators from TIA-603.
 *
 *  Two dummy tones are at each end of the spectrum.  It was noted during testing that when a tone was not
 *  present, the algorithm would drift to the lowest one, and we don't want a false detection when in fact there
 *  is no tone at all. Testing never revealed the highest tone falsely detected, but want completeness.
 */
public enum CTCSSCode
{
    UNKNOWNL(65.0f, "UKNL", "65.0"),
    TONE_670(67.0f, "XZ", "67.0"),
    TONE_693(69.3f, "WZ", "69.3"),
    TONE_719(71.9f, "XA", "71.9"),
    TONE_744(74.4f, "WA", "74.4"),
    TONE_770(77.0f, "XB", "77.0"),
    TONE_797(79.7f, "WB", "79.7"),
    TONE_825(82.5f, "YZ", "82.5"),
    TONE_854(85.4f, "YA", "85.4"),
    TONE_885(88.5f, "YB", "88.5"),
    TONE_915(91.5f, "ZZ", "91.5"),
    TONE_948(94.8f, "ZA", "94.8"),
    TONE_974(97.4f, "ZB", "97.4"),
    TONE_1000(100.0f, "1Z", "100.0"),
    TONE_1035(103.5f, "1A", "103.5"),
    TONE_1072(107.2f, "1B", "107.2"),
    TONE_1109(110.9f, "2Z", "110.9"),
    TONE_1148(114.8f, "2A", "114.8"),
    TONE_1188(118.8f, "2B", "118.8"),
    TONE_1230(123.0f, "3Z", "123.0"),
    TONE_1273(127.3f, "3A", "127.3"),
    TONE_1318(131.8f, "3B", "131.8"),
    TONE_1365(136.5f, "4Z", "136.5"),
    TONE_1413(141.3f, "4A", "141.3"),
    TONE_1462(146.2f, "4B", "146.2"),
    TONE_1514(151.4f, "5Z", "151.4"),
    TONE_1567(156.7f, "5A", "156.7"),
    TONE_1622(162.2f, "5B", "162.2"),
    TONE_1679(167.9f, "6Z", "167.9"),
    TONE_1738(173.8f, "6A", "173.8"),
    TONE_1799(179.9f, "6B", "179.9"),
    TONE_1862(186.2f, "7Z", "186.2"),
    TONE_1928(192.8f, "7A", "192.8"),
    TONE_2035(203.5f, "M1", "203.5"),
    TONE_2065(206.5f, "M2", "206.5"),
    TONE_2107(210.7f, "M3", "210.7"),
    TONE_2181(218.1f, "M4", "218.1"),
    TONE_2257(225.7f, "M5", "225.7"),
    TONE_2291(229.1f, "M6", "229.1"),
    TONE_2336(233.6f, "M7", "233.6"),
    TONE_2418(241.8f, "8Z", "241.8"),
    TONE_2503(250.3f, "9Z", "250.3"),
    TONE_2541(254.1f, "0Z", "254.1"),
    UNKNOWNH(260.0f, "UKNH", "260.0");
    /**
     * Set of all standard (non-UNKNOWN) CTCSS codes
     */
    public static final Set<CTCSSCode> STANDARD_CODES;
    public static final Set<CTCSSCode> DETECTING_CODES;

    static
    {
        EnumSet<CTCSSCode> codes = EnumSet.allOf(CTCSSCode.class);
        DETECTING_CODES = java.util.Collections.unmodifiableSet(codes);
        codes.remove(UNKNOWNL);
        codes.remove(UNKNOWNH);
        STANDARD_CODES = java.util.Collections.unmodifiableSet(codes);
    }

    private final float mFrequency;
    private final String mDesignator;
    private final String mFrequencyLabel;

    CTCSSCode(float frequency, String designator, String frequencyLabel)
    {
        mFrequency = frequency;
        mDesignator = designator;
        mFrequencyLabel = frequencyLabel;
    }

    /**
     * Tone frequency in Hz
     */
    public float getFrequency()
    {
        return mFrequency;
    }

    /**
     * EIA/TIA standard designator (e.g. "XZ", "1A", "M7")
     */
    public String getDesignator()
    {
        return mDesignator;
    }

    /**
     * Display string showing frequency and designator
     */
    public String getDisplayString()
    {
        return mFrequencyLabel + " Hz (" + mDesignator + ")";
    }

    @Override
    public String toString()
    {
    //    return mFrequencyLabel + " Hz";
        return getDisplayString();
    }

    /**
     * Finds the CTCSSCode closest to the given frequency within ±1 Hz tolerance.
     * @param frequency in Hz
     * @return matching code or UNKNOWN if no match
     */
    public static CTCSSCode fromFrequency(float frequency)
    {
        CTCSSCode best = UNKNOWNH;
        float bestDelta = Float.MAX_VALUE;

        for(CTCSSCode code : STANDARD_CODES)
        {
            float delta = Math.abs(code.mFrequency - frequency);
            if(delta < bestDelta)
            {
                bestDelta = delta;
                best = code;
            }
        }

        return bestDelta <= 1.0f ? best : UNKNOWNH;
    }
}
