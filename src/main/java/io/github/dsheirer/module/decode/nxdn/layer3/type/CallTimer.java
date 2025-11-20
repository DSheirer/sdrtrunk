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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

/**
 * Call Timer values enumeration
 */
public enum CallTimer
{
    UNSPECIFIED("UNSPECIFIED"),
    CT1("15 SECONDS"),
    CT2("30 SECONDS"),
    CT3("45 SECONDS"),
    CT4("60 SECONDS"),
    CT5("75 SECONDS"),
    CT6("90 SECONDS"),
    CT7("105 SECONDS"),
    CT8("120 SECONDS"),
    CT9("135 SECONDS"),
    CT10("150 SECONDS"),
    CT11("165 SECONDS"),
    CT12("180 SECONDS"),
    CT13("210 SECONDS"),
    CT14("240 SECONDS"),
    CT15("270 SECONDS"),
    CT16("300 SECONDS"),
    CT17("330 SECONDS"),
    CT18("360 SECONDS"),
    CT19("390 SECONDS"),
    CT20("420 SECONDS"),
    CT21("450 SECONDS"),
    CT22("480 SECONDS"),
    CT23("510 SECONDS"),
    CT24("540 SECONDS"),
    CT25("570 SECONDS"),
    CT26("600 SECONDS");

    private String mLabel;

    /**
     * Constructs an instance
     * @param label to display
     */
    CallTimer(String label)
    {
        mLabel = label;
    }

    public static CallTimer fromValue(int value)
    {
        return switch(value)
        {
            case 1 -> CT1;
            case 2 -> CT2;
            case 3 -> CT3;
            case 4 -> CT4;
            case 5 -> CT5;
            case 6 -> CT6;
            case 7 -> CT7;
            case 8 -> CT8;
            case 9 -> CT9;
            case 10 -> CT10;
            case 11 -> CT11;
            case 12 -> CT12;
            case 13 -> CT13;
            case 14 -> CT14;
            case 15 -> CT15;
            case 16 -> CT16;
            case 17 -> CT17;
            case 18 -> CT18;
            case 19 -> CT19;
            case 20 -> CT20;
            case 21 -> CT21;
            case 22 -> CT22;
            case 23 -> CT23;
            case 24 -> CT24;
            case 25 -> CT25;
            case 26 -> CT26;
            default -> UNSPECIFIED;
        };
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
