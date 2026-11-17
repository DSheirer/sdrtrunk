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

package io.github.dsheirer.module.decode.moto;

/**
 * De-emphasis applied to analog voice on a Motorola Type II traffic channel.
 *
 * Analog voice is transmitted with pre-emphasis.  De-emphasis restores the balance and lowers the noise bursts that
 * simulcast carrier nulls produce, at the cost of treble.  More de-emphasis hides more of the noise and dulls the
 * voice more, so the choice is made by ear for the system.  Every setting other than none also removes audio above
 * 3 kHz.
 */
public enum AnalogDeemphasis
{
    NONE("None", 0),
    US_75("Light (75 µs)", 75),
    US_150("Medium (150 µs)", 150),
    US_200("Medium-firm (200 µs)", 200),
    US_300("Firm (300 µs)", 300),
    US_750("Heavy (750 µs)", 750);

    private final String mLabel;
    private final int mTimeConstantMicroseconds;

    AnalogDeemphasis(String label, int timeConstantMicroseconds)
    {
        mLabel = label;
        mTimeConstantMicroseconds = timeConstantMicroseconds;
    }

    /**
     * Time constant in microseconds, or zero for no de-emphasis.
     */
    public int getTimeConstantMicroseconds()
    {
        return mTimeConstantMicroseconds;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
