/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

/**
 * Standard CTCSS (Continuous Tone-Coded Squelch System) codes.
 * Also known as PL (Private Line) by Motorola or Channel Guard by GE/Ericsson.
 */
public enum CTCSSCode
{
    // Standard EIA CTCSS Tone Set
    TONE_XZ(67.0f, "XZ"),
    TONE_WZ(69.3f, "WZ"),
    TONE_XA(71.9f, "XA"),
    TONE_WA(74.4f, "WA"),
    TONE_XB(77.0f, "XB"),
    TONE_WB(79.7f, "WB"),
    TONE_YZ(82.5f, "YZ"),
    TONE_YA(85.4f, "YA"),
    TONE_YB(88.5f, "YB"),
    TONE_ZZ(91.5f, "ZZ"),
    TONE_ZA(94.8f, "ZA"),
    TONE_ZB(97.4f, "ZB"),
    TONE_1Z(100.0f, "1Z"),
    TONE_1A(103.5f, "1A"),
    TONE_1B(107.2f, "1B"),
    TONE_2Z(110.9f, "2Z"),
    TONE_2A(114.8f, "2A"),
    TONE_2B(118.8f, "2B"),
    TONE_3Z(123.0f, "3Z"),
    TONE_3A(127.3f, "3A"),
    TONE_3B(131.8f, "3B"),
    TONE_4Z(136.5f, "4Z"),
    TONE_4A(141.3f, "4A"),
    TONE_4B(146.2f, "4B"),
    TONE_5Z(151.4f, "5Z"),
    TONE_5A(156.7f, "5A"),
    TONE_5B(162.2f, "5B"),
    TONE_6Z(167.9f, "6Z"),
    TONE_6A(173.8f, "6A"),
    TONE_6B(179.9f, "6B"),
    TONE_7Z(186.2f, "7Z"),
    TONE_7A(192.8f, "7A"),
    TONE_7B(199.5f, "7B"),
    TONE_M1(203.5f, "M1"),
    TONE_M2(206.5f, "M2"),
    TONE_M3(210.7f, "M3"),
    TONE_M4(218.1f, "M4"),
    TONE_M5(225.7f, "M5"),
    TONE_M6(229.1f, "M6"),
    TONE_M7(233.6f, "M7"),
    TONE_M8(241.8f, "M8"),
    TONE_M9(250.3f, "M9"),
    TONE_0Z(254.1f, "0Z"),

    UNKNOWN(0.0f, "UNK");

    private final float mFrequency;
    private final String mCode;

    /**
     * Standard CTCSS codes for UI selection (excludes UNKNOWN)
     */
    public static final EnumSet<CTCSSCode> STANDARD_CODES = EnumSet.of(
        TONE_XZ, TONE_WZ, TONE_XA, TONE_WA, TONE_XB, TONE_WB,
        TONE_YZ, TONE_YA, TONE_YB, TONE_ZZ, TONE_ZA, TONE_ZB,
        TONE_1Z, TONE_1A, TONE_1B, TONE_2Z, TONE_2A, TONE_2B,
        TONE_3Z, TONE_3A, TONE_3B, TONE_4Z, TONE_4A, TONE_4B,
        TONE_5Z, TONE_5A, TONE_5B, TONE_6Z, TONE_6A, TONE_6B,
        TONE_7Z, TONE_7A, TONE_7B, TONE_M1, TONE_M2, TONE_M3,
        TONE_M4, TONE_M5, TONE_M6, TONE_M7, TONE_M8, TONE_M9, TONE_0Z
    );

    /**
     * Constructor
     * @param frequency in Hz
     * @param code standard designation
     */
    CTCSSCode(float frequency, String code)
    {
        mFrequency = frequency;
        mCode = code;
    }

    /**
     * @return frequency in Hz
     */
    public float getFrequency()
    {
        return mFrequency;
    }

    /**
     * @return standard code designation (e.g., "XZ", "M1")
     */
    public String getCode()
    {
        return mCode;
    }

    /**
     * @return formatted display string
     */
    public String getDisplayString()
    {
        if(this == UNKNOWN)
        {
            return "Unknown";
        }
        return String.format("%.1f Hz (%s)", mFrequency, mCode);
    }

    @Override
    public String toString()
    {
        return getDisplayString();
    }

    /**
     * Lookup CTCSS code by frequency (with 0.5 Hz tolerance)
     * @param frequency to lookup
     * @return matching CTCSSCode or UNKNOWN
     */
    public static CTCSSCode fromFrequency(float frequency)
    {
        return fromFrequency(frequency, 0.5f);
    }

    /**
     * Lookup CTCSS code by frequency with specified tolerance
     * @param frequency to lookup
     * @param tolerance acceptable deviation in Hz
     * @return matching CTCSSCode or UNKNOWN
     */
    public static CTCSSCode fromFrequency(float frequency, float tolerance)
    {
        for(CTCSSCode code : values())
        {
            if(code != UNKNOWN && Math.abs(code.mFrequency - frequency) <= tolerance)
            {
                return code;
            }
        }
        return UNKNOWN;
    }

    /**
     * Lookup CTCSS code by code string
     * @param codeString to lookup (e.g., "XZ", "M1")
     * @return matching CTCSSCode or UNKNOWN
     */
    public static CTCSSCode fromCode(String codeString)
    {
        if(codeString == null || codeString.isEmpty())
        {
            return UNKNOWN;
        }

        for(CTCSSCode code : values())
        {
            if(code.mCode.equalsIgnoreCase(codeString))
            {
                return code;
            }
        }
        return UNKNOWN;
    }
}
