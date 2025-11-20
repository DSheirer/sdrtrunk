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
 * NXDN channel bandwidth
 */
public enum Bandwidth
{
    BW_6_25("6.25 kHz 4800BPS", TransmissionMode.M4800),
    BW_12_5("12.5 kHz 9600BPS", TransmissionMode.M9600);

    private final String mLabel;
    private final TransmissionMode mTransmissionMode;

    /**
     * Constructs an instance
     * @param label to display
     * @param transmissionMode for the channel
     */
    Bandwidth(String label, TransmissionMode transmissionMode)
    {
        mLabel = label;
        mTransmissionMode = transmissionMode;
    }

    /**
     * Transmission mode for the channel.
     */
    public TransmissionMode getTransmissionMode()
    {
        return mTransmissionMode;
    }

    /**
     * Utility method to lookup the bandwidth from a value.
     * @param value to lookup
     * @return matching bandwidth or 12.5 as a default
     */
    public static Bandwidth fromValue(int value)
    {
        return value == 0 ? BW_6_25 : BW_12_5;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
