/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.airspy.hf;

/**
 * Attenuation levels for the Airspy HF+ tuner
 */
public enum Attenuation
{
    A0(0, "0 dB"),
    A1(1, "6 dB"),
    A2(2, "12 dB"),
    A3(3, "18 dB"),
    A4(4, "24 dB"),
    A5(5, "30 dB"),
    A6(6, "36 dB"),
    A7(7, "42 dB"),
    A8(8, "48 dB");

    private int mValue;
    private String mLabel;

    /**
     * Constructs an instance
     * @param value for the attenuation level
     * @param label to display in the UI
     */
    Attenuation(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Byte value setting
     * @return value
     */
    public short getValue()
    {
        return (short)mValue;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Lookup the attenuation entry from the index value.
     * @param value to lookup
     * @return attenuation.
     */
    public static Attenuation fromValue(int value)
    {
        for(Attenuation attenuation: Attenuation.values())
        {
            if(attenuation.mValue == value)
            {
                return attenuation;
            }
        }

        return Attenuation.A0;
    }
}
