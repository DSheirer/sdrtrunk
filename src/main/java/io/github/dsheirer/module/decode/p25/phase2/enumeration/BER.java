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
 * Bit Error Rate (BER)
 */
public enum BER
{
    B0("0 - 0.08%"),
    B1("0.08-0.12%"),
    B2("0.12-0.18%"),
    B3("0.18-0.27%"),
    B4("0.27-0.39% "),
    B5("0.39-0.57%"),
    B6("0.57-0.84%"),
    B7("0.84-1.25%"),
    B8("1.25-1.35%"),
    B9("1.35-2.7%"),
    B10("2.7-3.9%"),
    B11("3.9-5.7%"),
    B12("5.7-8.4%"),
    B13("8.4-12.5%"),
    B14(">12.5%"),
    UNUSED("UNUSED");

    private String mLabel;

    BER(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    public static BER fromValue(int value)
    {
        if(0 <= value && value <= 15)
        {
            return BER.values()[value];
        }

        return UNUSED;
    }
}
