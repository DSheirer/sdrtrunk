/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.p25.phase2.enumeration;

/**
 * Indicates the RF level adjustment command to a mobile radio
 */
public enum RFLevel
{
    UNKNOWN("UNKNOWN"),
    MINUS_15("-15 dBm"),
    MINUS_12("-12 dBm"),
    MINUS_9("-9 dBm"),
    MINUS_6("-6 dBm"),
    MINUS_3("-3 dBm"),
    NONE("0 dBm"),
    PLUS_3("+3 dBm"),
    PLUS_6("+6 dBm"),
    PLUS_9("+9 dBm"),
    PLUS_12("+12 dBm"),
    PLUS_15("+15 dBm"),
    PLUS_18("+18 dBm"),
    PLUS_21("+21 dBm"),
    PLUS_24("+24 dBm"),
    PLUS_27("+27 dBm");

    private String mLabel;

    RFLevel(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    public static RFLevel fromValue(int value)
    {
        if(0 <= value && value <= 15)
        {
            return RFLevel.values()[value];
        }

        return UNKNOWN;
    }
}
