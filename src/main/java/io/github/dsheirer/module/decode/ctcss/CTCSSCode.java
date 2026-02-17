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

package io.github.dsheirer.module.decode.ctcss;

import java.util.EnumSet;
import java.util.Set;

/**
 * Standard CTCSS (Continuous Tone-Coded Squelch System) tones.
 * Also known as PL (Private Line) tones (Motorola trademark).
 *
 * Frequencies and EIA/TIA standard designators from TIA-603.
 */
public enum CTCSSCode
{
    TONE_XZ(67.0f, "XZ", "67.0"),
    TONE_WZ(69.3f, "WZ", "69.3"),
    TONE_XA(71.9f, "XA", "71.9"),
    TONE_WA(74.4f, "WA", "74.4"),
    TONE_XB(77.0f, "XB", "77.0"),
    TONE_WB(79.7f, "WB", "79.7"),
    TONE_YZ(82.5f, "YZ", "82.5"),
    TONE_YA(85.4f, "YA", "85.4"),
    TONE_YB(88.5f, "YB", "88.5"),
    TONE_ZZ(91.5f, "ZZ", "91.5"),
    TONE_ZA(94.8f, "ZA", "94.8"),
    TONE_ZB(97.4f, "ZB", "97.4"),
    TONE_1Z(100.0f, "1Z", "100.0"),
    TONE_1A(103.5f, "1A", "103.5"),
    TONE_1B(107.2f, "1B", "107.2"),
    TONE_2Z(110.9f, "2Z", "110.9"),
    TONE_2A(114.8f, "2A", "114.8"),
    TONE_2B(118.8f, "2B", "118.8"),
    TONE_3Z(123.0f, "3Z", "123.0"),
    TONE_3A(127.3f, "3A", "127.3"),
    TONE_3B(131.8f, "3B", "131.8"),
    TONE_4Z(136.5f, "4Z", "136.5"),
    TONE_4A(141.3f, "4A", "141.3"),
    TONE_4B(146.2f, "4B", "146.2"),
    TONE_5Z(151.4f, "5Z", "151.4"),
    TONE_5A(156.7f, "5A", "156.7"),
    TONE_5B(162.2f, "5B", "162.2"),
    TONE_6Z(167.9f, "6Z", "167.9"),
    TONE_6A(173.8f, "6A", "173.8"),
    TONE_6B(179.9f, "6B", "179.9"),
    TONE_7Z(186.2f, "7Z", "186.2"),
    TONE_7A(192.8f, "7A", "192.8"),
    TONE_M1(203.5f, "M1", "203.5"),
    TONE_M2(206.5f, "M2", "206.5"),
    TONE_M3(210.7f, "M3", "210.7"),
    TONE_M4(218.1f, "M4", "218.1"),
    TONE_M5(225.7f, "M5", "225.7"),
    TONE_M6(229.1f, "M6", "229.1"),
    TONE_M7(233.6f, "M7", "233.6"),
    TONE_8Z(241.8f, "8Z", "241.8"),
    TONE_9Z(250.3f, "9Z", "250.3"),
    TONE_0Z(254.1f, "0Z", "254.1"),
    UNKNOWN(0.0f, "??", "Unknown");

    /**
     * Set of all standard (non-UNKNOWN) CTCSS codes
     */
    public static final Set<CTCSSCode> STANDARD_CODES;

    static
    {
        EnumSet<CTCSSCode> codes = EnumSet.allOf(CTCSSCode.class);
        codes.remove(UNKNOWN);
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
        return mFrequencyLabel + " Hz";
    }

    /**
     * Finds the CTCSSCode closest to the given frequency within ±1 Hz tolerance.
     * @param frequency in Hz
     * @return matching code or UNKNOWN if no match
     */
    public static CTCSSCode fromFrequency(float frequency)
    {
        CTCSSCode best = UNKNOWN;
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

        return bestDelta <= 1.0f ? best : UNKNOWN;
    }
}
